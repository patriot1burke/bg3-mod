package org.baldurs.forge.model;

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
    int armorClass,
    RootTemplate rootTemplate,
    StatsArchive.Stat stat
) {


}
