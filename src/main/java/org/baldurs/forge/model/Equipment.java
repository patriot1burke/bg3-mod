package org.baldurs.forge.model;

import java.util.Set;

import org.baldurs.forge.scanner.RootTemplate;
import org.baldurs.forge.scanner.StatsArchive;

public record Equipment(
    String id,
    EquipmentType type,
    EquipmentSlot slot,
    Rarity rarity,
    String name,
    String description,
    String boostDescription,
    String icon,
    String weaponType,
    String armorType,
    int armorClass,
    Set<String> weaponProperties,
    RootTemplate rootTemplate,
    StatsArchive.Stat stat
) {


}
