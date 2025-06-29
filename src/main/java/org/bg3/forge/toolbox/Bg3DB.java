package org.bg3.forge.toolbox;

import org.bg3.forge.scanner.Bg3Library;

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
            library.getLocalizationCollector().scan(Path.of("/mnt/c/Users/patri/mods/bg3-localization/Localization/English/english.xml"));
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> scanBoosts() {
        Map<String, Set<String>> boosts = new HashMap<>();

        Set<String> macros = library.statsCollector.collectAttributesValues("Boosts");
        macros.addAll(library.statsCollector.collectAttributesValues("PassivesOnEquip"));
        macros.addAll(library.statsCollector.collectAttributesValues("DefaultBoosts"));
        macros.addAll(library.statsCollector.collectAttributesValues("BoostsOnEquipMainHand"));
        macros.addAll(library.statsCollector.collectAttributesValues("PassivesOffHand"));
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
                functions.add(function + "(" + param + ")");
            }
        }
        functions.sort(Comparator.naturalOrder());
        return functions;
    }
}