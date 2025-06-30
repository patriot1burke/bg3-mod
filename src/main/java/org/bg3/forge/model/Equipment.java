package org.bg3.forge.model;

import org.bg3.forge.scanner.RootTemplateCollector;
import org.bg3.forge.scanner.StatsCollector;

public record Equipment(
    String id,
    EquipmentType type,
    EquipmentSlot slot,
    String name,
    String description,
    RootTemplateCollector.RootTemplate rootTemplate,
    StatsCollector.Stat stat
) {

}
