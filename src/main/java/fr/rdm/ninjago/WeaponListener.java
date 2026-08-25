package fr.rdm.ninjago;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

public final class WeaponListener implements Listener {
    private final NinjagoPlugin plugin;
    private final WeaponManager weapons;
    private final Map<UUID, Map<WeaponType, Long>> cooldowns = new HashMap<>();
    private final NamespacedKey shurikenKey;
    private final NamespacedKey fireballKey;

    public WeaponListener(NinjagoPlugin plugin, WeaponManager weapons) {
        this.plugin = plugin;
        this.weapons = weapons;
        this.shurikenKey = new NamespacedKey(plugin, "ice_shuriken_projectile");
        this.fireballKey = new NamespacedKey(plugin, "fire_sword_projectile");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        WeaponType type = weapons.getType(item);
        if (type == null) return;

        switch (type) {
            case SWORD_OF_FIRE -> fire(event.getPlayer());
            case NUNCHUCKS_OF_LIGHTNING -> lightning(event.getPlayer());
            case SCYTHE_OF_QUAKES -> quake(event.getPlayer());
            case SHURIKENS_OF_ICE -> { }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (weapons.getType(event.getBow()) != WeaponType.SHURIKENS_OF_ICE) return;
        if (!ready(player, WeaponType.SHURIKENS_OF_ICE)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1.0f, 1.5f);
        Vector base = player.getEyeLocation().getDirection().normalize();
        double spread = plugin.getConfig().getDouble("powers.shurikens.spread", 0.22);
        for (double offset : new double[]{-spread, 0.0, spread}) {
            Snowball ball = player.launchProjectile(Snowball.class);
            Vector direction = base.clone().add(new Vector(offset, 0, 0).rotateAroundY(Math.toRadians(player.getLocation().getYaw()))).normalize();
            ball.setVelocity(direction.multiply(2.2));
            ball.getPersistentDataContainer().set(shurikenKey, PersistentDataType.BYTE, (byte) 1);
        }
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getEyeLocation(), 18, 0.25, 0.25, 0.25, 0.03);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof SmallFireball fireball
                && fireball.getPersistentDataContainer().has(fireballKey, PersistentDataType.BYTE)) {
            if (event.getHitEntity() instanceof LivingEntity living) {
                Object shooter = fireball.getShooter();
                living.damage(plugin.getConfig().getDouble("powers.fire.damage", 6.0), shooter instanceof Entity e ? e : null);
                living.setFireTicks(Math.max(living.getFireTicks(), plugin.getConfig().getInt("powers.fire.fire_ticks", 60)));
            }
            Location hit = fireball.getLocation();
            hit.getWorld().spawnParticle(Particle.FLAME, hit, 30, 0.35, 0.35, 0.35, 0.03);
            return;
        }

        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!snowball.getPersistentDataContainer().has(shurikenKey, PersistentDataType.BYTE)) return;

        Location loc = snowball.getLocation();
        World world = loc.getWorld();
        world.spawnParticle(Particle.SNOWFLAKE, loc, 40, 0.6, 0.6, 0.6, 0.05);
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.6f);
        Object shooter = snowball.getShooter();
        for (Entity entity : world.getNearbyEntities(loc, 2.0, 2.0, 2.0)) {
            if (entity instanceof LivingEntity living && living != shooter) {
                living.damage(plugin.getConfig().getDouble("powers.shurikens.damage", 4.0), shooter instanceof Entity e ? e : null);
                living.setFreezeTicks(Math.min(living.getMaxFreezeTicks(), living.getFreezeTicks() + 60));
            }
        }
    }

    private void fire(Player player) {
        if (!ready(player, WeaponType.SWORD_OF_FIRE)) return;
        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        fireball.getPersistentDataContainer().set(fireballKey, PersistentDataType.BYTE, (byte) 1);
        fireball.setIsIncendiary(false);
        fireball.setYield(0);
        fireball.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.5));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.1f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 20, 0.3, 0.3, 0.3, 0.02);
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
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("cooldowns." + type.id(), 3L) * 1000L;
        Map<WeaponType, Long> map = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(WeaponType.class));
        long last = map.getOrDefault(type, 0L);
        if (now - last < cooldown) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("Pouvoir en recharge..."));
            return false;
        }
        map.put(type, now);
        return true;
    }
}
