package org.bg3.forge.model;

import java.util.List;

import org.bg3.forge.scanner.RootTemplateCollector;
import org.bg3.forge.scanner.StatsCollector;

import com.fasterxml.jackson.databind.ObjectMapper;

public record Equipment(
    String id,
    EquipmentType type,
    EquipmentSlot slot,
    Rarity rarity,
    String name,
    String description,
    String boostDescription,
    RootTemplateCollector.RootTemplate rootTemplate,
    StatsCollector.Stat stat
) {


}
