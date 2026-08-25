package fr.rdm.ninjago;

import org.bukkit.Material;

public enum WeaponType {
    SWORD_OF_FIRE("sword_of_fire", "Épée de Feu", Material.NETHERITE_SWORD, 1),
    SHURIKENS_OF_ICE("shurikens_of_ice", "Shurikens de Glace", Material.BOW, 2),
    NUNCHUCKS_OF_LIGHTNING("nunchucks_of_lightning", "Nunchakus de la Foudre", Material.NETHERITE_SWORD, 3),
    SCYTHE_OF_QUAKES("scythe_of_quakes", "Faux des Tremblements", Material.NETHERITE_AXE, 4);

    private final String id;
    private final String displayName;
    private final Material material;
    private final int customModelData;

    WeaponType(String id, String displayName, Material material, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Material material() { return material; }
    public int customModelData() { return customModelData; }

    public static WeaponType fromId(String input) {
        for (WeaponType type : values()) {
            if (type.id.equalsIgnoreCase(input)) return type;
        }
        return null;
    }
}
