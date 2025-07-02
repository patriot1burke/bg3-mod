package org.bg3.forge.toolbox;

import java.util.HashMap;
import java.util.Map;

import org.bg3.forge.model.Equipment;
import org.bg3.forge.model.EquipmentSlot;
import org.bg3.forge.model.EquipmentType;
import org.bg3.forge.scanner.RootTemplateCollector;
import org.bg3.forge.scanner.StatsCollector;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class EquipmentDB {
    @Inject
    LibraryService libraryService;

    @Inject
    DescriptionService descriptionService;

    Map<String, Equipment> equipmentDB = new HashMap<>();

    public void start(@Observes StartupEvent event) {
        buildEquipment();
    }


    private void buildEquipment() {
        Map<String, StatsCollector.Stat> armors = libraryService.library().statsCollector.getArmor();
        for (StatsCollector.Stat armor : armors.values()) {
            String id = armor.name;
            EquipmentType type = EquipmentType.valueOf(armor.type);
            EquipmentSlot slot = EquipmentSlot.valueOf(armor.getField("Slot"));
            RootTemplateCollector.RootTemplate rootTemplate = libraryService.library().getRootTemplateCollector().templates.get(armor.getField("RootTemplate"));
            String name = libraryService.library().getLocalizationCollector().getLocalization(rootTemplate.DisplayName);
            String description = libraryService.library().getLocalizationCollector().getLocalization(rootTemplate.Description);
            StringBuilder boostDescription = new StringBuilder();
            descriptionService.armor(armor, (desc) -> boostDescription.append("<p>").append(desc).append("</p>"));
            Equipment equipment = new Equipment(id, type, slot, name, description, boostDescription.toString(), rootTemplate, armor);
            equipmentDB.put(id, equipment);
            
        }
        
    }

}
