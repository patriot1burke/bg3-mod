package org.bg3.forge.model;

import org.bg3.forge.scanner.StatsCollector;

public record Equipment(
    String id,
    EquipmentType type,
    EquipmentSlot slot,
    Rarity rarity,
    String name,
    String description,
    String boostDescription,
    RootTemplate rootTemplate,
    StatsCollector.Stat stat
) {


}
