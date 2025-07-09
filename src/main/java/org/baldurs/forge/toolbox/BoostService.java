package org.baldurs.forge.toolbox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.function.Consumer;

import org.baldurs.forge.scanner.StatsArchive;
import org.baldurs.forge.toolbox.MacroService.Macro;

import dev.langchain4j.agent.tool.Tool;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BoostService {
    @Inject
    LibraryService bg3DB;
    @Inject
    MacroService macroService;

    public interface BoostWriter {
        void writeGrouping(String name);
        void write(String description);
        void write(String icon, String displayName, String description);
        void writeSpell(StatsArchive.Stat spell);
        void writeUnknown(String macro);
    }
    public BoostWriter html() {
        return new HtmlBoostWriter();
    }
    public BoostWriter text() {
        return new TextBoostWriter();
    }

    private class HtmlBoostWriter implements BoostWriter {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public void writeGrouping(String name) {
            sb.append("<p><i>").append(name).append("</i></p>");
        }

        @Override
        public void write(String description) {
            sb.append("<p>").append(description).append("</p>");
        }

        @Override
        public void write(String icon, String displayName, String description) {
            sb.append("<p>");
            if (icon != null) {
                writeSpellImage(icon, displayName == null ? "" : displayName);
            }
            if (displayName != null) {
                sb.append("<span class='boost-name'>").append(displayName).append(":</span>");
            }
            sb.append(description).append("</p>");
        }

        private void writeSpellImage(String icon, String displayName) {
            if (displayName == null) {
                displayName = "Spell";
            }
            sb.append("<img class='spell-icon' src='__skill_icon_path__").append(icon).append("__skill_icon_suffix__' alt='").append(displayName).append("'>");
        }

        @Override
        public void writeSpell(StatsArchive.Stat spell) {
            String icon = spell.getField("Icon");
            String displayName = statDisplayName(spell);
            writeSpellImage(icon, displayName);
            sb.append("<div class='spell-name'>").append(displayName).append("</div>");
        }

        @Override
        public void writeUnknown(String macro) {
            sb.append("<p><i>Unknown macro: ").append(macro).append("</i></p>");
        }

        public String toString() {
            return sb.toString();
        }
    }

    private class TextBoostWriter implements BoostWriter {
        private final StringWriter sw = new StringWriter();
        private final PrintWriter pw = new PrintWriter(sw);

        @Override
        public void writeGrouping(String grouping) {
            pw.println(grouping);
        }

        @Override
        public void write(String description) {
            pw.println(description);
        }

        @Override
        public void write(String icon, String displayName, String description) {
            pw.println(displayName + ": " + description);
        }

        @Override
        public void writeSpell(StatsArchive.Stat spell) {
            String displayName = statDisplayName(spell);
            pw.printf("Spell: %s\n", displayName);
        }

        @Override
        public void writeUnknown(String macro) {
            pw.println("Unknown macro: " + macro);
        }

        public String toString() {
            pw.flush();
            return sw.toString();
        }
    }

    public String statDescription(StatsArchive.Stat stat) {
        String handle = stat.getField("Description");
        if (handle == null) {
            return null;
        }
        String description = bg3DB.library().getLocalizationCollector().getLocalization(handle);
        if (description != null) {
            String params = stat.getField("DescriptionParams");
            if (params == null) {
                return description;
            }
            String[] paramArray = params.split(";");
            for (int i = 0; i < paramArray.length; i++) {
                String param = paramArray[i];
                param = DescriptionParam.param(param);
                description = description.replace("[" + (i + 1) + "]", param);
            }
        }
        return description;
    }

    public String statDisplayName(StatsArchive.Stat stat) {
        if (stat == null) {
            return null;
        }
        String handle = stat.getField("DisplayName");
        if (handle == null) {
            return null;
        }
        return bg3DB.library().getLocalizationCollector().getLocalization(handle);
    }

    public BoostService passive(String name, BoostWriter writer) {
        StatsArchive.Stat stat = bg3DB.library().statsCollector.getByName(name);
        if (stat == null) {
            return this;
        }
        String description = statDescription(stat);
        if (description != null) {
            String displayName = statDisplayName(stat);
            String icon = stat.getField("Icon");
            writer.write(icon, displayName, description);
        } else {
            String macro = stat.getField("Boosts");
            macros(macro, writer);
        }
        return this;
    }

    public void macros(String macro, BoostWriter writer) {
        if (macro != null) {
            for (String boost : MacroService.splitMacro(macro)) {
                if (macroService.isMacro(boost)) {
                    macroService.writeMacro(boost, writer);
                } else {
                    passive(boost, writer);
                }
            }
        }
    }

    public BoostService weapon(String weaponName, BoostWriter writer) {
        StatsArchive.Stat stat = bg3DB.library().statsCollector.getByName(weaponName);
        return weapon(stat, writer);
    }

    public BoostService weapon(StatsArchive.Stat stat, BoostWriter writer) {
        macros(stat.getField("PassivesOnEquip"), writer);
        String mainHand = stat.getField("PassivesMainHand");
        if (mainHand != null) {
            writer.writeGrouping("<i>Main Hand Only</i>");
            macros(mainHand, writer);
        }
        String offHand = stat.getField("PassivesOffHand");
        if (offHand != null) {
            writer.writeGrouping("<i>Off Hand Only</i>");
            macros(offHand, writer);
        }
        macros(stat.getField("Boosts"), writer);
        macros(stat.getField("DefaultBoosts"), writer);
        macros(stat.getField("BoostsOnEquipMainHand"), writer);
        return this;
    }

    public BoostService stat(StatsArchive.Stat stat, BoostWriter writer) {
        if (stat.type.equals(StatsArchive.Library.ARMOR_TYPE)) {
            armor(stat, writer);
        } else if (stat.type.equals(StatsArchive.Library.WEAPON_TYPE)) {
            weapon(stat, writer);
        }
        return this;
    }

    public BoostService armor(String armorName, BoostWriter writer) {
        StatsArchive.Stat stat = bg3DB.library().statsCollector.getByName(armorName);
        return armor(stat, writer);
    }

    public BoostService armor(StatsArchive.Stat stat, BoostWriter writer) {
        macros(stat.getField("PassivesOnEquip"), writer);
        macros(stat.getField("Boosts"), writer);
        return this;
    }

}
