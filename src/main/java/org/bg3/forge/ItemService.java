package org.bg3.forge;

import java.util.List;

import org.jboss.logging.Logger;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ItemService {
  private static Logger logger = Logger.getLogger(ItemService.class);
  @Inject
  EmbeddingStore<TextSegment> embeddingStore;

  @Inject 
  EmbeddingModel embeddingModel;

  @Transactional
  public List<Item> query(String queryString) {
    logger.infof("Querying for: %s", queryString);
    Embedding embedding = embeddingModel.embed(queryString).content();
    EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
    .queryEmbedding(embedding)
    .minScore(0.9)
    .maxResults(5)
    .build();

    return embeddingStore.search(request).matches().stream().map(m -> {
      Long id = m.embedded().metadata().getLong("id");
      Item movie = Item.findById(id);
      return movie;
    }).toList();

  }
}

