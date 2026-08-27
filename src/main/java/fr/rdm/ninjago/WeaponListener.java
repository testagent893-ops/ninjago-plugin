package fr.rdm.ninjago;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

public final class WeaponListener implements Listener {
    private final NinjagoPlugin plugin;
    private final WeaponManager weapons;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    private enum Kind { FIRE, ICE }

    public WeaponListener(NinjagoPlugin plugin, WeaponManager weapons) {
        this.plugin = plugin;
        this.weapons = weapons;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        WeaponType type = weapons.getType(item);
        if (type == null) return;
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            event.setCancelled(true);
            spinjutsu(player, type);
            return;
        }

        switch (type) {
            case SWORD_OF_FIRE -> fire(player);
            case NUNCHUCKS_OF_LIGHTNING -> lightning(player);
            case SCYTHE_OF_QUAKES -> quake(player);
            case SHURIKENS_OF_ICE -> { }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (weapons.getType(event.getBow()) != WeaponType.SHURIKENS_OF_ICE) return;
        event.setCancelled(true);
        if (player.isSneaking()) return; // déjà géré par onInteract (spinjutsu)
        if (!ready(player, WeaponType.SHURIKENS_OF_ICE)) return;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1.0f, 1.5f);
        Vector base = player.getEyeLocation().getDirection().normalize();
        double spread = plugin.getConfig().getDouble("powers.shurikens.spread", 0.22);
        for (double offset : new double[]{-spread, 0.0, spread}) {
            Vector direction = base.clone()
                    .add(new Vector(offset, 0, 0).rotateAroundY(Math.toRadians(player.getLocation().getYaw())))
                    .normalize();
            launchCustomProjectile(player, player.getEyeLocation(), direction, 2.0,
                    "ninjago", "shuriken_projectile", 0.45f, Kind.ICE);
        }
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getEyeLocation(), 18, 0.25, 0.25, 0.25, 0.03);
    }

    private void fire(Player player) {
        if (!ready(player, WeaponType.SWORD_OF_FIRE)) return;
        Vector direction = player.getEyeLocation().getDirection().normalize();
        launchCustomProjectile(player, player.getEyeLocation(), direction, 1.4,
                "ninjago", "fireball_projectile", 0.5f, Kind.FIRE);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.1f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 20, 0.3, 0.3, 0.3, 0.02);
    }

    /**
     * Spinjutsu : boost de vitesse + poussée des ennemis proches, thème visuel/effet
     * différent selon l'arme équipée. Déclenché en étant accroupi + clic droit.
     */
    private void spinjutsu(Player player, WeaponType type) {
        String path = "spinjutsu." + type.id() + ".";
        long cooldown = plugin.getConfig().getLong(path + "cooldown", 8L) * 1000L;
        if (!consumeCooldown(player, type.id() + "_spin", cooldown)) return;

        int duration = plugin.getConfig().getInt(path + "speed_duration", 3) * 20;
        int amplifier = Math.max(0, plugin.getConfig().getInt(path + "speed_amplifier", 2) - 1);
        double radius = plugin.getConfig().getDouble(path + "push_radius", 3.5);
        double strength = plugin.getConfig().getDouble(path + "push_strength", 1.3);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier));

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && entity != player) {
                Vector push = entity.getLocation().toVector().subtract(player.getLocation().toVector());
                if (push.lengthSquared() < 0.01) push = player.getLocation().getDirection().clone();
                push.normalize().multiply(strength).setY(0.35);
                living.setVelocity(living.getVelocity().add(push));
                if (type == WeaponType.SHURIKENS_OF_ICE) {
                    int slow = plugin.getConfig().getInt(path + "slow_ticks", 30);
                    living.setFreezeTicks(Math.min(living.getMaxFreezeTicks(), living.getFreezeTicks() + slow));
                }
            }
        }

        Particle particle = switch (type) {
            case SWORD_OF_FIRE -> Particle.FLAME;
            case SHURIKENS_OF_ICE -> Particle.SNOWFLAKE;
            case NUNCHUCKS_OF_LIGHTNING -> Particle.ELECTRIC_SPARK;
            case SCYTHE_OF_QUAKES -> Particle.BLOCK;
        };

        Location center = player.getLocation().add(0, 1, 0);
        int points = 28;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle) * (radius * 0.55);
            double z = Math.sin(angle) * (radius * 0.55);
            Location p = center.clone().add(x, 0, z);
            if (particle == Particle.BLOCK) {
                player.getWorld().spawnParticle(particle, p, 1, 0, 0, 0, 0, Material.DIRT.createBlockData());
            } else {
                player.getWorld().spawnParticle(particle, p, 1, 0, 0, 0, 0.01);
            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
    }

    /**
     * Projectile 100% custom : uniquement un ItemDisplay avec notre texture, sans aucune
     * vraie entité vanilla (Snowball/Fireball) en parallèle. Ça évite tout dédoublement visuel
     * et n'affecte aucune texture partagée avec les autres joueurs ou les mobs.
     */
    private void launchCustomProjectile(Player shooter, Location start, Vector direction, double speed,
                                         String namespace, String itemKey, float scale, Kind kind) {
        ItemStack icon = new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();
        meta.setItemModel(new NamespacedKey(namespace, itemKey));
        icon.setItemMeta(meta);

        ItemDisplay display = start.getWorld().spawn(start, ItemDisplay.class, d -> {
            d.setItemStack(icon);
            d.setBillboard(Display.Billboard.CENTER);
            org.joml.Vector3f s = new org.joml.Vector3f(scale, scale, scale);
            d.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0, 0, 0),
                    new org.joml.AxisAngle4f(0, 0, 0, 1),
                    s,
                    new org.joml.AxisAngle4f(0, 0, 0, 1)));
        });

        Vector step = direction.clone().multiply(speed);
        Predicate<Entity> filter = e -> e instanceof LivingEntity && e != shooter;

        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;
            final int maxTicks = 40;

            @Override
            public void run() {
                ticks++;
                World world = current.getWorld();

                RayTraceResult entityHit = world.rayTraceEntities(current, step, step.length(), 0.35, filter);
                RayTraceResult blockHit = world.rayTraceBlocks(current, step, step.length());

                if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity target) {
                    Location impact = entityHit.getHitPosition().toLocation(world);
                    onHit(shooter, target, impact, kind);
                    display.remove();
                    this.cancel();
                    return;
                }

                Location next = current.clone().add(step);

                if (blockHit != null || ticks >= maxTicks) {
                    Location impact = blockHit != null ? blockHit.getHitPosition().toLocation(world) : next;
                    onImpact(impact, kind);
                    display.remove();
                    this.cancel();
                    return;
                }

                current = next;
                display.teleport(current);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void onHit(Player shooter, LivingEntity target, Location loc, Kind kind) {
        World world = loc.getWorld();
        if (kind == Kind.FIRE) {
            double radius = plugin.getConfig().getDouble("powers.fire.radius", 1.5);
            for (Entity nearby : world.getNearbyEntities(loc, radius, radius, radius)) {
                if (nearby instanceof LivingEntity living && nearby != shooter) {
                    living.damage(plugin.getConfig().getDouble("powers.fire.damage", 6.0), shooter);
                    living.setFireTicks(Math.max(living.getFireTicks(), plugin.getConfig().getInt("powers.fire.fire_ticks", 60)));
                }
            }
            world.spawnParticle(Particle.FLAME, loc, 30, 0.35, 0.35, 0.35, 0.03);
        } else {
            target.damage(plugin.getConfig().getDouble("powers.shurikens.damage", 4.0), shooter);
            target.setFreezeTicks(Math.min(target.getMaxFreezeTicks(), target.getFreezeTicks() + 60));
            world.spawnParticle(Particle.SNOWFLAKE, loc, 40, 0.6, 0.6, 0.6, 0.05);
            world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.6f);
        }
    }

    private void onImpact(Location loc, Kind kind) {
        World world = loc.getWorld();
        if (kind == Kind.FIRE) {
            world.spawnParticle(Particle.FLAME, loc, 15, 0.2, 0.2, 0.2, 0.02);
        } else {
            world.spawnParticle(Particle.SNOWFLAKE, loc, 15, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void lightning(Player player) {
        if (!ready(player, WeaponType.NUNCHUCKS_OF_LIGHTNING)) return;
        double radius = plugin.getConfig().getDouble("powers.lightning.radius", 5.0);
        double damage = plugin.getConfig().getDouble("powers.lightning.damage", 5.0);
        int hit = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && entity != player) {
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                living.damage(damage, player);
                hit++;
                if (hit >= 3) break;
            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.6f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 50, 1.5, 1.0, 1.5, 0.15);
    }

    private void quake(Player player) {
        if (!ready(player, WeaponType.SCYTHE_OF_QUAKES)) return;
        double radius = plugin.getConfig().getDouble("powers.scythe.radius", 5.0);
        double damage = plugin.getConfig().getDouble("powers.scythe.damage", 6.0);
        Location center = player.getLocation();
        for (Entity entity : player.getNearbyEntities(radius, 2.5, radius)) {
            if (entity instanceof LivingEntity living && entity != player) {
                Vector push = entity.getLocation().toVector().subtract(center.toVector());
                if (push.lengthSquared() < 0.01) push = player.getLocation().getDirection().clone();
                push.normalize().multiply(1.2).setY(0.55);
                living.setVelocity(push);
                living.damage(damage, player);
            }
        }
        player.getWorld().spawnParticle(Particle.BLOCK, center, 70, radius / 2.0, 0.15, radius / 2.0, Material.DIRT.createBlockData());
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
    }

    private boolean ready(Player player, WeaponType type) {
        long cooldown = plugin.getConfig().getLong("cooldowns." + type.id(), 3L) * 1000L;
        return consumeCooldown(player, type.id(), cooldown);
    }

    private boolean consumeCooldown(Player player, String key, long cooldownMs) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        long last = map.getOrDefault(key, 0L);
        if (now - last < cooldownMs) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("Pouvoir en recharge..."));
            return false;
        }
        map.put(key, now);
        return true;
    }
}
