package org.bg3.forge;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

@ApplicationScoped
public class ItemLoader {
  @Inject
  EmbeddingStore embeddingStore;
  
  @Inject
  EmbeddingModel embeddingModel;

//  public void load(@Observes StartupEvent event) throws Exception {
  public void load() throws Exception {

    Log.info("Loading items...");

    embeddingStore.removeAll();

    EmbeddingStoreIngestor ingester = EmbeddingStoreIngestor.builder()
    .embeddingModel(embeddingModel)
    .embeddingStore(embeddingStore)
    .build();

    List<Document> docs = new ArrayList<>();
    List<ForgeItem> items = ForgeItem.loadJson();
    for (ForgeItem item : items) {
      Log.infof("Loading item: %s", item.name);
      Item i = new Item();
      i.title = item.name;
      String description = "";
      description += "This is a " + item.type;
      if (item.slot != null) {
        description += " for " + item.slot + ".";
      } else {
        description += ".";
      }
      if (item.weaponAttributes != null) {
        description += "The weapon is a " + item.weaponAttributes + ".";
      }
      description += " " + item.description;

      i.description = description;
      Log.infof("Description  : %s", i.description);
      save(i);

      Metadata metadata = Metadata.from(Map.of("id", i.id, "title", i.title));
      Document document = Document.from(i.description, metadata);
      docs.add(document);
    }

    Log.info("Ingesting items...");
    ingester.ingest(docs);
    Log.info("Application initalized!");
  }

  @Transactional
  public Item save(Item m) {
    m.persist();
    return m;
  }

}

