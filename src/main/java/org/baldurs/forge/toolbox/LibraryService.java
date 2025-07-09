package org.baldurs.forge.toolbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.baldurs.forge.model.StatModel;
import org.baldurs.forge.scanner.BaldursArchive;
import org.baldurs.forge.scanner.RootTemplate;
import org.baldurs.forge.scanner.StatsArchive;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LibraryService {
    private BaldursArchive library = new BaldursArchive();
    private boolean initialized = false;

    @Inject
    @ConfigProperty(name = "baldurs.forge.scanner.cache.root.path", defaultValue = "/home/bburke/projects/baldurs-forge/cache")
    private String rootPath;

    @PostConstruct
    public void init() {
        scanFiles();
    }

    private void scanFiles() {
        if (initialized)
            return;
        initialized = true;
        try {
            Log.info("Loading game data...");
            Path root = Path.of(rootPath);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            Path statsPath = root.resolve("stats.json");
            if (Files.exists(statsPath)) {
                library.getStatsCollector().load(statsPath);
            } else {
                library.getStatsCollector()
                        .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data"))
                        .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data"))
                        .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data"))
                        .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/Gustav/Stats/Generated/Data"))
                        .save(statsPath);
            }

            Path rootTemplatesPath = root.resolve("root-templates.json");
            if (Files.exists(rootTemplatesPath)) {
                library.getRootTemplateCollector().load(rootTemplatesPath);
            } else {
                library.getRootTemplateCollector()
                        .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/RootTemplates/_merged.lsx"))
                        .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/RootTemplates/_merged.lsx"))
                        .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/RootTemplates/_merged.lsx"))
                        .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/Gustav/RootTemplates/_merged.lsx"))
                        .save(rootTemplatesPath);
            }

            Path localizationPath = root.resolve("localization.json");
            if (Files.exists(localizationPath)) {
                library.getLocalizationCollector().load(localizationPath);
            } else {
                library.getLocalizationCollector()
                        .scan(Path.of("/mnt/c/Users/patri/mods/bg3-localization/Localization/English/english.xml"));
                library.getLocalizationCollector().save(localizationPath);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BaldursArchive library() {
        return library;
    }

    @Tool("Find a root template by stat name")
    public RootTemplate findRootTemplateByStatName(String statName) {
        Log.infof("Finding root template for stat: %s", statName);
        for (RootTemplate rootTemplate : library.getRootTemplateCollector().templates.values()) {
            if (statName.equals(rootTemplate.Stats)) {
                return rootTemplate;
            }
        }
        return null;
    }

    @Tool("Get or find or show a stat by name")
    public StatModel getStatByName(String name, @P(value = "Add parent data?", required = false) boolean parentData) {
        StatsArchive.Stat stat = library.statsCollector.getByName(name);
        if (stat == null) {
            return null;
        }
        if (parentData) {
            return new StatModel(stat.name, stat.type, stat.using, stat.aggregateData());
        } else {
            return new StatModel(stat.name, stat.type, stat.using, stat.data);
        }
    }

    @Tool("Get all possible values for a Stat attribute")
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
            String[] tokens = MacroService.splitMacro(macro);
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