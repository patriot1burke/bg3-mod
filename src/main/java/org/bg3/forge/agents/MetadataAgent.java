package org.bg3.forge.agents;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import org.bg3.forge.model.EquipmentFilter;
import org.bg3.forge.model.EquipmentFilters;

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
