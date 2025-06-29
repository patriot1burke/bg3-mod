package org.bg3.forge.agents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterAiService()
public interface ForgeAgent {

    @SystemMessage("""
        You are a helpful assistant that can answer questions about Baldur's Gate 3 items.
        From the following data (in JSON format):
        {json}
        Answer user questions in natural langauge using the data provided above.  When listing items, only specify the name of it.
        """)
    String answer(@UserMessage String question, String json);
}
