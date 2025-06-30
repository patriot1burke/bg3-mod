package org.bg3.forge.toolbox;

import org.bg3.forge.model.Equipment;
import org.bg3.forge.model.EquipmentSlot;
import org.bg3.forge.model.EquipmentType;
import org.bg3.forge.scanner.Bg3Library;
import org.bg3.forge.scanner.RootTemplateCollector;
import org.bg3.forge.scanner.StatsCollector;

import dev.langchain4j.agent.tool.Tool;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

@ApplicationScoped
public class Bg3DB {
    private Bg3Library library = new Bg3Library();

    public void start(@Observes StartupEvent event) {
        try {
            Log.info("Starting Bg3DB");

            library.getStatsCollector()
            .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Armor.txt"))
            .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data/Armor.txt"))
            .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data/Armor.txt"))
            .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Weapon.txt"))
            .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data/Weapon.txt"))
            .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data/Weapon.txt"))
            
            .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Passive.txt"))
            .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data/Passive.txt"))
            .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data/Passive.txt"))
            
                    ;

                    library.getRootTemplateCollector().scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/RootTemplates/_merged.lsx"));
                    library.getRootTemplateCollector().scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/RootTemplates/_merged.lsx"));
                    library.getLocalizationCollector().scan(Path.of("/mnt/c/Users/patri/mods/bg3-localization/Localization/English/english.xml"));
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    Map<String, Equipment> equipment = new HashMap<>();

    public void buildEquipment() {
        Map<String, StatsCollector.Stat> armor = library.statsCollector.getArmor();
        Map<String, StatsCollector.Stat> weapons = library.statsCollector.getWeapons();
        for (StatsCollector.Stat stat : armor.values()) {
            String id = stat.name;
            EquipmentType type = EquipmentType.valueOf(stat.type);
            EquipmentSlot slot = EquipmentSlot.valueOf(stat.getField("Slot"));
            RootTemplateCollector.RootTemplate rootTemplate = library.getRootTemplateCollector().templates.get(stat.getField("RootTemplate"));
            
        }
        
    }

    public List<String> getStatAttributeValues(String attributeName) {
        Set<String> values = library.statsCollector.collectAttributesValues(attributeName);
        return values.stream().sorted().toList();
    }

    @Tool("Get all possible boost function signatures")
    public List<String> getAllBoostFunctionSignatures() {
        Map<String, Set<String>> boosts = new HashMap<>();

        Set<String> macros = library.statsCollector.collectAttributesValues("Boosts");
        macros.addAll(library.statsCollector.collectAttributesValues("PassivesOnEquip"));
        macros.addAll(library.statsCollector.collectAttributesValues("DefaultBoosts"));
        macros.addAll(library.statsCollector.collectAttributesValues("BoostsOnEquipMainHand"));
        macros.addAll(library.statsCollector.collectAttributesValues("PassivesOffHand"));
        return getFunctions(boosts, macros);
    }

    private List<String> getFunctions(Map<String, Set<String>> boosts, Set<String> macros) {
        for (String macro : macros) {
            String[] tokens = macro.split(";");
            for (String token : tokens) {
                String expression = token;
                int index = token.indexOf(":");
                if (index > 0) {
                    expression = token.substring(index + 1);
                }
                index = expression.indexOf('(');
                if (index <= 0) {
                    continue;
                }
                String functionName = expression.substring(0, index);
                int closingIndex = expression.lastIndexOf(')');
                String params = expression.substring(index + 1, closingIndex);
                params = params.trim();
                Set<String> functionParams = boosts.computeIfAbsent(functionName, k -> new LinkedHashSet<>());
                if (params.isEmpty()) {
                    functionParams.add("");
                    continue;
                }
                String[] paramTokens = params.split(",");
                String parameters = null;
                for (String param : paramTokens) {
                    if (parameters != null) parameters += ",";
                    if (parameters == null) parameters = "";
                    if (param.trim().matches("^-?\\d+$")) {
                        parameters += "number";
                    } else if (param.trim().matches("^\\d+d\\d+$")) {
                        parameters += "die_roll";
                    } else {
                        parameters += param.trim();
                    }
                }
                functionParams.add(parameters);
            }
        }
        List<String> functions = new ArrayList<>();
        for (String function : boosts.keySet()) {
            for (String param : boosts.get(function)) {
                // Remove quotes from quoted strings
                String unquotedParam = param.replaceAll("^\"|\"$", "");
                
                // Extract substring within parentheses
                // Matches: functionName(arguments) -> extracts "arguments"
                String parenthesesPattern = "\\((.*?)\\)";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(parenthesesPattern);
                java.util.regex.Matcher matcher = pattern.matcher(function + "(" + unquotedParam + ")");
                
                if (matcher.find()) {
                    String extractedContent = matcher.group(1); // Group 1 contains the content inside parentheses
                    System.out.println("Extracted: " + extractedContent);
                }
                
                // Regular expression to match function call and extract function name and parameters
                // Matches: functionName(arg1, arg2, arg3) -> extracts function name and all parameters
                String functionCallPattern = "(\\w+)\\s*\\(([^)]*)\\)";
                Pattern functionPattern = Pattern.compile(functionCallPattern);
                java.util.regex.Matcher funcMatcher = functionPattern.matcher(function + "(" + unquotedParam + ")");
                
                if (funcMatcher.find()) {
                    String functionName = funcMatcher.group(1); // Group 1: function name
                    String parameters = funcMatcher.group(2);   // Group 2: parameters
                    System.out.println("Function: " + functionName + ", Parameters: " + parameters);
                }
                
                functions.add(function + "(" + unquotedParam + ")");
            }
        }
        functions.sort(Comparator.naturalOrder());
        return functions;
    }
}