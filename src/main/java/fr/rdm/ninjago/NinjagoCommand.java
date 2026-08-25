package fr.rdm.ninjago;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class NinjagoCommand implements CommandExecutor, TabCompleter {
    private final NinjagoPlugin plugin;
    private final WeaponManager weapons;

    public NinjagoCommand(NinjagoPlugin plugin, WeaponManager weapons) {
        this.plugin = plugin;
        this.weapons = weapons;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(Component.text("/ninjago give <joueur> <arme>", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/ninjago list", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/ninjago remove <joueur> <arme>", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/ninjago reload", NamedTextColor.GOLD));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            for (WeaponType type : WeaponType.values()) {
                sender.sendMessage(Component.text(type.id() + " - " + type.displayName(), NamedTextColor.YELLOW));
            }
            return true;
        }

        if (!sender.hasPermission("ninjago.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(Component.text("Configuration rechargée.", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            Player target = Bukkit.getPlayerExact(args[1]);
            WeaponType type = WeaponType.fromId(args[2]);
            if (target == null || type == null) {
                sender.sendMessage(Component.text("Joueur ou arme invalide.", NamedTextColor.RED));
                return true;
            }
            target.getInventory().addItem(weapons.create(type));
            sender.sendMessage(Component.text("Arme donnée à " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Tu as reçu : " + type.displayName(), NamedTextColor.GOLD));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove") && args.length >= 3) {
            Player target = Bukkit.getPlayerExact(args[1]);
            WeaponType type = WeaponType.fromId(args[2]);
            if (target == null || type == null) {
                sender.sendMessage(Component.text("Joueur ou arme invalide.", NamedTextColor.RED));
                return true;
            }
            int removed = 0;
            for (int slot = 0; slot < target.getInventory().getSize(); slot++) {
                ItemStack item = target.getInventory().getItem(slot);
                if (weapons.getType(item) == type) {
                    target.getInventory().setItem(slot, null);
                    removed++;
                }
            }
            sender.sendMessage(Component.text("Armes retirées : " + removed, NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Utilise /ninjago help", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("give", "list", "remove", "reload", "help"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
            return filter(Arrays.stream(WeaponType.values()).map(WeaponType::id).toList(), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(p)) out.add(value);
        return out;
    }
}
