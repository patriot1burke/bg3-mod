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
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

@ApplicationScoped
public class LibraryService {
    private Bg3Library library = new Bg3Library();
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        scanFiles();
    }

    private void scanFiles() {
        if (initialized)
            return;
        initialized = true;
        try {
            Log.info("Starting Bg3DB");

            library.getStatsCollector()
                    .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/Gustav/Stats/Generated/Data"))


            ;

            library.getRootTemplateCollector()
                    .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/RootTemplates/_merged.lsx"));
            library.getRootTemplateCollector()
                    .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/RootTemplates/_merged.lsx"));
            library.getLocalizationCollector()
                    .scan(Path.of("/mnt/c/Users/patri/mods/bg3-localization/Localization/English/english.xml"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Bg3Library library() {
        return library;
    }

    public List<String> getStatAttributeValues(String attributeName) {
        scanFiles();
        Set<String> values = library.statsCollector.collectAttributesValues(attributeName);
        List<String> list = new ArrayList<>(values);
        Collections.sort(list);
        return list;
    }

    @Tool("Get all possible boost function signatures")
    public List<String> getAllBoostFunctionSignatures() {
        scanFiles();
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
            String[] tokens = MacroDescriptionService.splitMacro(macro);
            for (String token : tokens) {
                try {
                    String expression = token.trim();
                    int index = token.indexOf(":");
                    if (index > 0) {
                        expression = token.substring(index + 1);
                    }
                    index = expression.indexOf('(');
                    if (index <= 0) {
                        continue;
                    }
                    String functionName = expression.substring(0, index).trim();
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
                        param = param.trim();
                        if (parameters != null)
                            parameters += ",";
                        if (parameters == null)
                            parameters = "";
                        if (param.matches("^-?\\d+$")) {
                            parameters += "number";
                        } else if (param.matches("^\\d+d\\d+$")) {
                            parameters += "die_roll";
                        } else {
                            parameters += param;
                        }
                    }
                    functionParams.add(parameters);
                } catch (Exception e) {
                    Log.error("Error parsing boost function: '" + macro + "' '" + token + "'", e);
                    throw e;
                }
            }
        }
        List<String> functions = new ArrayList<>();
        for (String function : boosts.keySet()) {
            for (String param : boosts.get(function)) {
                functions.add(function + "(" + param + ")");
            }
        }
        Collections.sort(functions, Comparator.naturalOrder());
        return functions;
    }
}