package org.baldurs.forge.agents;

import java.util.function.Supplier;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class StrictJsonSchemaChatModelProvider implements Supplier<ChatModel> {
    @Override
    public ChatModel get() {
        return OpenAiChatModel.builder() 
                .apiKey(System.getenv("QUARKUS_LANGCHAIN4J_OPENAI_API_KEY"))
                .modelName("gpt-4o-2024-08-06")
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .strictJsonSchema(true) 
                .logRequests(true)
                .logResponses(true)
                .build();
    }

}
