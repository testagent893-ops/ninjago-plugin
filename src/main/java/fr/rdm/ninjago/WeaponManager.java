package fr.rdm.ninjago;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class WeaponManager {
    private final NinjagoPlugin plugin;

    public WeaponManager(NinjagoPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack create(WeaponType type) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.displayName(), NamedTextColor.GOLD));
        meta.lore(java.util.List.of(
                Component.text("Arme d'Or", NamedTextColor.YELLOW),
                Component.text("Ninjago SMP", NamedTextColor.GRAY)
        ));
        meta.setCustomModelData(type.customModelData());
        meta.getPersistentDataContainer().set(plugin.weaponKey(), PersistentDataType.STRING, type.id());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public WeaponType getType(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.weaponKey(), PersistentDataType.STRING);
        return id == null ? null : WeaponType.fromId(id);
    }
}
