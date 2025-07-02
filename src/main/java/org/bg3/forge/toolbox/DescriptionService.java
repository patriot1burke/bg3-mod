package org.bg3.forge.toolbox;

import java.util.function.Consumer;

import org.bg3.forge.scanner.StatsCollector;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DescriptionService {
    @Inject
    LibraryService bg3DB;
    @Inject
    MacroDescriptionService macroDescriptionService;

    public String getDescription(String handle) {
        String value = bg3DB.library().getLocalizationCollector().getLocalization(handle);
        // todo, replace markup with html
        return value;
    }

    public String statDescription(StatsCollector.Stat stat) {
        String description = stat.getField("Description");
        if (description != null) {
            String params = stat.getField("DescriptionParams");
            String[] paramArray = params.split(";");
            for (int i = 0; i < paramArray.length; i++) {
                String param = paramArray[i];
                param = DescriptionParam.param(param);
                description = description.replace("[" + (i + 1) + "]", param);
            }
        }
        return description;
    }

    public DescriptionService passive(String name, Consumer<String> writer) {
        StatsCollector.Stat stat = bg3DB.library().statsCollector.getByName(name);
        if (stat == null) {
            return this;
        }
        String description = statDescription(stat);
        if (description != null) {
            writer.accept(description);
        } else {
            String macro = stat.getField("Boosts");
            macros(macro, writer);
        }
        return this;
    }

    public void macros(String macro, Consumer<String> writer) {
        if (macro != null) {
            for (String boost : macro.split(";")) {
                String macroDescription = macroDescriptionService.description(boost);
                if (macroDescription != null) {
                    writer.accept(macroDescription);
                } else {
                    passive(boost, writer);
                }
            }
        }
    }

    public DescriptionService weapon(String weaponName, Consumer<String> writer) {
        StatsCollector.Stat stat = bg3DB.library().statsCollector.getByName(weaponName);
        return weapon(stat, writer);
    }

    public DescriptionService weapon(StatsCollector.Stat stat, Consumer<String> writer) {
        macros(stat.getField("PassivesOnEquip"), writer);
        String mainHand = stat.getField("PassivesMainHand");
        if (mainHand != null) {
            writer.accept("<i>Main Hand Only</i>");
            macros(mainHand, writer);
        }
        String offHand = stat.getField("PassivesOffHand");
        if (offHand != null) {
            writer.accept("<i>Off Hand Only</i>");
            macros(offHand, writer);
        }
        macros(stat.getField("DefaultBoosts"), writer);
        return this;
    }

    public DescriptionService armor(String armorName, Consumer<String> writer) {
        StatsCollector.Stat stat = bg3DB.library().statsCollector.getByName(armorName);
        return armor(stat, writer);
    }

    public DescriptionService armor(StatsCollector.Stat stat, Consumer<String> writer) {
        macros(stat.getField("PassivesOnEquip"), writer);
        macros(stat.getField("Boosts"), writer);
        return this;
    }

}
