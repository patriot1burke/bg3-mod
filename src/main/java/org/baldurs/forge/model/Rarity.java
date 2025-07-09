package org.baldurs.forge.model;

public enum Rarity {
    Common,
    Uncommon,
    Rare,
    VeryRare,
    Legendary,
    ;

    public static Rarity fromString(String rarity) {
        if (rarity == null) {
            return Common;
        }
        return Rarity.valueOf(rarity);
    }
}
