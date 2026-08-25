package fr.rdm.ninjago;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class NinjagoPlugin extends JavaPlugin {
    private NamespacedKey weaponKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        weaponKey = new NamespacedKey(this, "weapon");

        WeaponManager weaponManager = new WeaponManager(this);
        getServer().getPluginManager().registerEvents(new WeaponListener(this, weaponManager), this);

        PluginCommand command = getCommand("ninjago");
        if (command != null) {
            NinjagoCommand executor = new NinjagoCommand(this, weaponManager);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getComponentLogger().info(Component.text("NinjagoSMP activé.", NamedTextColor.GOLD));
    }

    public NamespacedKey weaponKey() {
        return weaponKey;
    }
}
