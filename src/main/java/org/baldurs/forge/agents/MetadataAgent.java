package org.baldurs.forge.agents;

import jakarta.enterprise.context.ApplicationScoped;

import org.baldurs.forge.model.EquipmentFilters;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@ApplicationScoped
@RegisterAiService(chatLanguageModelSupplier = StrictJsonSchemaChatModelProvider.class)
public interface MetadataAgent {

    @SystemMessage("""
            Your task is to extract metadata from a natural language query about Baldur's Gate 3 weapons and armor.
                    """)
    EquipmentFilters answer(@UserMessage String query);
}
