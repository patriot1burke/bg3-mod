package org.baldurs.forge.toolbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.baldurs.forge.agents.ForgeAgent;
import org.baldurs.forge.agents.MetadataAgent;
import org.baldurs.forge.model.Equipment;
import org.baldurs.forge.model.EquipmentFilter;
import org.baldurs.forge.model.EquipmentFilters;
import org.baldurs.forge.model.EquipmentModel;
import org.baldurs.forge.model.EquipmentSlot;
import org.baldurs.forge.model.EquipmentType;
import org.baldurs.forge.model.Rarity;
import org.baldurs.forge.scanner.RootTemplate;
import org.baldurs.forge.scanner.RootTemplateArchive;
import org.baldurs.forge.scanner.StatsArchive;
import org.baldurs.forge.toolbox.BoostService.BoostWriter;
import org.baldurs.forge.util.FilterExpression;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EquipmentDB {
    @Inject
    LibraryService libraryService;

    @Inject
    BoostService boostService;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    MetadataAgent metadataAgent;

    @Inject
    ForgeAgent forgeAgent;

    Map<String, Equipment> equipmentDB = new HashMap<>();

    public void start(@Observes StartupEvent event) throws Exception {
        buildEquipment();
        load();
    }

    private void buildEquipment() {
        Log.info("Building equipment database");
        Map<String, StatsArchive.Stat> armors = libraryService.library().statsCollector.getArmor();
        for (StatsArchive.Stat armor : armors.values()) {
            addEquipment(armor);
        }
        Map<String, StatsArchive.Stat> weapons = libraryService.library().statsCollector.getWeapons();
        for (StatsArchive.Stat weapon : weapons.values()) {
            addEquipment(weapon);
        }
        Log.info("Added " + equipmentDB.size() + " to equipment database");

    }

    private void addEquipment(StatsArchive.Stat item) {
        String id = item.name;
        EquipmentType type = EquipmentType.valueOf(item.type);
        EquipmentSlot slot = EquipmentSlot.fromString(item.getField("Slot"));
        if (slot == EquipmentSlot.Unknown) {
            Log.debug("Unknown slot for " + id);
            return;
        }
        int armorClass = -1;
        if (type == EquipmentType.Armor && slot == EquipmentSlot.Breast) {
            String field = item.getField("ArmorClass");
            if (field != null && !field.isEmpty()) {
                armorClass = Integer.parseInt(field);
            }
        }
        Rarity rarity = Rarity.fromString(item.getField("Rarity"));
        RootTemplate rootTemplate = libraryService.library()
                .getRootTemplateCollector().templates.get(item.getField("RootTemplate"));
        if (rootTemplate == null) {
            Log.debug("No root template for " + id);
            return;
        }
        String displayName = rootTemplate.DisplayName;
        if (displayName == null || displayName.isEmpty()) {
            Log.debug("No display name for " + id);
            return;
        }
        String name = libraryService.library().getLocalizationCollector().getLocalization(displayName);
        if (name == null) {
            Log.debug("No name for " + id);
            return;
        }
        String description = "";
        if (rootTemplate.Description != null) {
            description = libraryService.library().getLocalizationCollector()
                    .getLocalization(rootTemplate.Description);
        } else {
            Log.debug("No description for " + id);
        }
        BoostWriter boostWriter = boostService.html();
        try {
            boostService.stat(item, boostWriter);
        } catch (Exception e) {
            throw new RuntimeException("Error processing boosts for " + id, e);
        }
        String boost = boostWriter.toString();
        //Log.infof("Boosts for %s: %s", id, boost);
        String icon = rootTemplate.resolveIcon();
        String weaponType = null;
        if (type == EquipmentType.Weapon) {
            String proficiencies = item.getField("Proficiency Group");
            if (proficiencies != null) {
                String[] profs = proficiencies.split(";");
                if (profs.length > 0) {
                    weaponType = profs[0].substring(0, profs[0].length() - 1);
                }
            }
        }
        String armorType = null;
        if (type == EquipmentType.Armor) {
            armorType = item.getField("ArmorType");
            if (armorType != null) {
                if (armorType.equals("None")) {
                    armorType = null;
                } else {
                    armorType = addSpacesBetweenCapitals(armorType);
                }
            }
        }
        Set<String> weaponProperties = new HashSet<>();
        if (type == EquipmentType.Weapon) {
            String properties = item.getField("Weapon Properties");
            if (properties != null) {
                String[] props = properties.split(";");
                for (String prop : props) {
                    weaponProperties.add(prop);
                }
            }
        }
        Equipment equipment = new Equipment(id, type, slot, rarity, name, description, boost, icon, weaponType, armorType, armorClass, weaponProperties, rootTemplate, item);
        equipmentDB.put(id, equipment);
    }

    private void load() throws Exception {

        Log.info("Loading items...");

        embeddingStore.removeAll();

        EmbeddingStoreIngestor ingester = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        int MAX_INGEST = 20;

        List<Document> docs = new ArrayList<>();
        for (Equipment item : equipmentDB.values()) {
            BoostWriter boostWriter = boostService.text();
            StatsArchive.Stat stat = libraryService.library().statsCollector.getByName(item.id());
            boostService.stat(stat, boostWriter);
            String boost = boostWriter.toString();
            Metadata metadata = Metadata.from(Map.of(
                    "id", item.id(), 
                    "type", item.type().name(), 
                    "slot", item.slot().name(), 
                    "rarity", item.rarity().name()));
            if (item.weaponType() != null) {
                metadata.put("weaponType", item.weaponType());
            }
            if (item.armorType() != null) {
                metadata.put("armorType", item.armorType());
            }
            String content = "Name: " + item.name() + "\n" +
                    "Type: " + item.type() + "\n" +
                    "Slot: " + item.slot() + "\n" +
                    "Rarity: " + item.rarity() + "\n";
            if (item.weaponType() != null) {
                content += "Weapon Type: " + item.weaponType() + "\n";
            }
            if (item.armorType() != null) {
                content += "Armor Type: " + item.armorType() + "\n";
            }
            if (item.weaponProperties() != null) {
                content += "Weapon Properties: " + item.weaponProperties() + "\n";
            }
            content +=
                    "Boosts: " + boost;
            //Log.info("\nid: " + item.id() + "\n" + content);
            Document document = Document.from(content, metadata);
            docs.add(document);
         }
        ingester.ingest(docs);

        Log.info("Ingested " + equipmentDB.size() + " items");
    }

    @Tool("Find an item in the equipment database by name")
    public EquipmentModel findByName(String name) {
        Log.infof("Finding by name: %s", name);
        Equipment equipment = equipmentDB.values().stream().filter(e -> e.name().equals(name)).findFirst().orElse(null);
        if (equipment == null) {
            return null;
        }
        Log.infof("Found: %s", equipment.name());
        return EquipmentModel.from(equipment);
    }

    public static record SearchResult(List<EquipmentModel> items, String summary) {}

    @Tool("Search or find or show items in the equipment database based on a natural language query")
    @Transactional
    public SearchResult searchAndSummary(String queryString) {
        Log.infof("searchAndSummary for: %s", queryString);
        List<EquipmentModel> models = search(queryString);
        String summary = forgeAgent.queryEquipment(queryString, EquipmentModel.toJson(models));
        return new SearchResult(models, summary);
    }

    @Transactional
    public List<EquipmentModel> search(String queryString) {
        Log.infof("Querying for: %s", queryString);
        // getting the filter is not very reliable or accurate
        // need to play with it more
        //Filter filter = getFilter(queryString);

        Embedding embedding = embeddingModel.embed(queryString).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                //.filter(filter.filter())
                .minScore(0.5)
                .maxResults(10)
                .build();

        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(request);
        Log.info("Search results: " + search.matches().size());
        List<Equipment> result = search.matches().stream().map(m -> {
            String id = m.embedded().metadata().getString("id");
            return equipmentDB.get(id);
        }).toList();

        return result.stream().map(EquipmentModel::from).toList();

    }

    private Filter getFilter(String queryString) {
        EquipmentFilters filters = metadataAgent.answer(queryString);
        FilterExpression filter = new FilterExpression();
        if (filters != null && filters.filters() != null && !filters.filters().isEmpty()) {
            for (EquipmentFilter eq : filters.filters()) {
                FilterExpression x = new FilterExpression();
                if (eq.type() != null) {
                    Filter typeFilter = new MetadataFilterBuilder("type").isEqualTo(eq.type().name());
                    x.and(typeFilter);
                }
                if (eq.slot() != null && eq.slot() != EquipmentSlot.Unknown) {
                    Filter slotFilter = new MetadataFilterBuilder("slot").isEqualTo(eq.slot().name());
                    x.and(slotFilter);
                }
                filter.or(x);
            }
        }
        return filter.filter();
    }

    /**
     * Utility method to find capital letters in a string
     * @param str the input string
     * @return a list of capital letters found in the string
     */
    public static List<Character> findCapitalLetters(String str) {
        List<Character> capitals = new ArrayList<>();
        if (str == null) {
            return capitals;
        }
        
        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                capitals.add(c);
            }
        }
        return capitals;
    }

    /**
     * Alternative method to find capital letters using regex
     * @param str the input string
     * @return a list of capital letters found in the string
     */
    public static List<Character> findCapitalLettersRegex(String str) {
        List<Character> capitals = new ArrayList<>();
        if (str == null) {
            return capitals;
        }
        
        // Using regex to find capital letters
        String capitalPattern = "[A-Z]";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(capitalPattern);
        java.util.regex.Matcher matcher = pattern.matcher(str);
        
        while (matcher.find()) {
            capitals.add(matcher.group().charAt(0));
        }
        return capitals;
    }

    /**
     * Method to get positions of capital letters in a string
     * @param str the input string
     * @return a map of character to list of positions where it appears
     */
    public static Map<Character, List<Integer>> findCapitalLetterPositions(String str) {
        Map<Character, List<Integer>> positions = new HashMap<>();
        if (str == null) {
            return positions;
        }
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                positions.computeIfAbsent(c, k -> new ArrayList<>()).add(i);
            }
        }
        return positions;
    }

    /**
     * Adds spaces between capital letters in a string
     * @param str the input string
     * @return the string with spaces added between capital letters
     */
    public static String addSpacesBetweenCapitals(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                // Add space before capital letter (except at the beginning)
                result.append(' ');
            }
            result.append(c);
        }
        return result.toString();
    }

}
