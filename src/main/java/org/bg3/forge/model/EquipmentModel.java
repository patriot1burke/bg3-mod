package org.bg3.forge.model;

public record EquipmentModel(
        String id,
        EquipmentType type,
        EquipmentSlot slot,
        Rarity rarity,
        String name,
        String description,
        String boostDescription) {

}
