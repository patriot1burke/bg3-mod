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
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import io.quarkus.logging.Log;
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
            EquipmentSlot slot = EquipmentSlot.valueOf(armor.getField("Slot"));
            Rarity rarity = Rarity.fromString(armor.getField("Rarity"));
            RootTemplateCollector.RootTemplate rootTemplate = libraryService.library()
                    .getRootTemplateCollector().templates.get(armor.getField("RootTemplate"));
            String name = libraryService.library().getLocalizationCollector().getLocalization(rootTemplate.DisplayName);
            String description = libraryService.library().getLocalizationCollector()
                    .getLocalization(rootTemplate.Description);
            StringBuilder boostDescription = new StringBuilder();
            descriptionService.armor(armor, (desc) -> boostDescription.append("<p>").append(desc).append("</p>"));
            Equipment equipment = new Equipment(id, type, slot, rarity, name, description, boostDescription.toString(),
                    rootTemplate, armor);
            equipmentDB.put(id, equipment);

        }
        Log.info("Building equipment embeddings");

    }

    private void load() throws Exception {

        Log.info("Loading items...");

        embeddingStore.removeAll();

        EmbeddingStoreIngestor ingester = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        List<Document> docs = new ArrayList<>();
        for (Equipment item : equipmentDB.values()) {
            Metadata metadata = Metadata.from(Map.of("id", item.id(), "type", item.type().name(), "slot",
                    item.slot().name(), "rarity", item.rarity().name()));
            String content = "Name: " + item.name() + "\n" +
                    "Type: " + item.type() + "\n" +
                    "Slot: " + item.slot() + "\n" +
                    "Rarity: " + item.rarity() + "\n" +
                    "Boosts: " + item.boostDescription();
            Document document = Document.from(content, metadata);
            docs.add(document);
        }

        Log.info("Ingesting items...");
        ingester.ingest(docs);
        Log.info("Application initalized!");
    }

    public List<Equipment> query(String queryString) {
        Log.infof("Querying for: %s", queryString);
        EquipmentFilters filters = metadataAgent.answer(queryString);
        FilterExpression filter = new FilterExpression();
        if (filters != null && !filters.filters().isEmpty()) {
            for (EquipmentFilter eq : filters.filters()) {
                FilterExpression x = new FilterExpression();
                if (eq.type() != null) {
                    Filter typeFilter = new MetadataFilterBuilder("type").isEqualTo(eq.type().name());
                    x.and(typeFilter);
                }
                if (eq.slot() != null) {
                    Filter slotFilter = new MetadataFilterBuilder("slot").isEqualTo(eq.slot().name());
                    x.and(slotFilter);
                }
                if (eq.rarity() != null) {
                    Filter rarityFilter = new MetadataFilterBuilder("rarity").isEqualTo(eq.rarity().name());
                    x.and(rarityFilter);
                }
                filter.or(x);
            }
        }

        Embedding embedding = embeddingModel.embed(queryString).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .filter(filter.filter())
                .minScore(0.75)
                .maxResults(5)
                .build();


        return embeddingStore.search(request).matches().stream().map(m -> {
            String id = m.embedded().metadata().getString("id");
            return equipmentDB.get(id);
        }).toList();

    }

}
