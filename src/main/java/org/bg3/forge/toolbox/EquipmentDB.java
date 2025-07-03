package org.bg3.forge.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bg3.forge.agents.MetadataAgent;
import org.bg3.forge.model.Equipment;
import org.bg3.forge.model.EquipmentFilter;
import org.bg3.forge.model.EquipmentFilters;
import org.bg3.forge.model.EquipmentSlot;
import org.bg3.forge.model.EquipmentType;
import org.bg3.forge.model.Rarity;
import org.bg3.forge.scanner.RootTemplateCollector;
import org.bg3.forge.scanner.StatsCollector;
import org.bg3.forge.util.FilterExpression;

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
    DescriptionService descriptionService;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    MetadataAgent metadataAgent;

    Map<String, Equipment> equipmentDB = new HashMap<>();

    public void start(@Observes StartupEvent event) throws Exception {
        buildEquipment();
        load();
    }

    private void buildEquipment() {
        Log.info("Building equipment database");
        Map<String, StatsCollector.Stat> armors = libraryService.library().statsCollector.getArmor();
        for (StatsCollector.Stat armor : armors.values()) {
            String id = armor.name;
            EquipmentType type = EquipmentType.valueOf(armor.type);
            EquipmentSlot slot = EquipmentSlot.fromString(armor.getField("Slot"));
            if (slot == EquipmentSlot.Unknown) {
                Log.debug("Unknown slot for " + id);
                continue;
            }
            Rarity rarity = Rarity.fromString(armor.getField("Rarity"));
            RootTemplateCollector.RootTemplate rootTemplate = libraryService.library()
                    .getRootTemplateCollector().templates.get(armor.getField("RootTemplate"));
            if (rootTemplate == null) {
                Log.debug("No root template for " + id);
                continue;
            }
            String displayName = rootTemplate.DisplayName;
            if (displayName == null || displayName.isEmpty()) {
                Log.debug("No display name for " + id);
                continue;
            }
            String name = libraryService.library().getLocalizationCollector().getLocalization(displayName);
            if (name == null) {
                Log.debug("No name for " + id);
                continue;
            }
            String description = "";
            if (rootTemplate.Description != null) {
                description = libraryService.library().getLocalizationCollector()
                        .getLocalization(rootTemplate.Description);
            } else {
                Log.debug("No description for " + id);
            }
            StringBuilder boostDescription = new StringBuilder();
            try {
                descriptionService.armor(armor, (desc) -> boostDescription.append("<p>").append(desc).append("</p>"));
            } catch (Exception e) {
                throw new RuntimeException("Error processing boosts for " + id, e);
            }
            String boost = boostDescription.toString();
            Equipment equipment = new Equipment(id, type, slot, rarity, name, description, boost,
                    rootTemplate, armor);
            equipmentDB.put(id, equipment);
            /*
             * if (boost.isEmpty()) {
             * continue;
             * } else {
             * Log.info("Adding " + id + " " + equipment.boostDescription());
             * //equipmentDB.put(id, equipment);
             * }
             */

        }
        Log.info("Added " + equipmentDB.size() + " to equipment database");

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
            Metadata metadata = Metadata.from(Map.of("id", item.id(), "type", item.type().name(), "slot",
                    item.slot().name(), "rarity", item.rarity().name()));
            String content = "Name: " + item.name() + "\n" +
                    "Type: " + item.type() + "\n" +
                    "Slot: " + item.slot() + "\n" +
                    "Rarity: " + item.rarity() + "\n" +
                    "Boosts: " + item.boostDescription();
            //Log.info("\nid: " + item.id() + "\n" + content);
            Document document = Document.from(content, metadata);
            docs.add(document);
         }
        ingester.ingest(docs);

        Log.info("Ingested " + equipmentDB.size() + " items");
    }

    @Transactional
    public List<Equipment> query(String queryString) {
        Log.infof("Querying for: %s", queryString);
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

        Embedding embedding = embeddingModel.embed(queryString).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                //.filter(filter.filter())
                .minScore(0.5)
                .maxResults(10)
                .build();

        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(request);
        Log.info("Search results: " + search.matches().size());
        return search.matches().stream().map(m -> {
            Log.info("Score: " + m.score());
            String id = m.embedded().metadata().getString("id");
            return equipmentDB.get(id);
        }).toList();

    }

}
