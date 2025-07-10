package org.baldurs.forge.model;

public enum EquipmentSlot {
    Unknown,
    Helmet,
    Breast,
    Gloves,
    Boots,
    Ring,
    Amulet,
    Cloak,
    Melee,
    Offhand,
    Ranged;

    public static EquipmentSlot fromString(String string) {
        if (string == null) {
            return Unknown;
        }
        if (string.equals("Melee Main Weapon")) {
            return Melee;
        } else if (string.equals("Ranged Main Weapon")) {
            return Ranged;
        } else if (string.equals("Melee Offhand Weapon")) {
            return Offhand;
        }
        try {
            return EquipmentSlot.valueOf(string);
        } catch (Exception e) {
            return Unknown;
        }
    }
}
