package org.bg3.forge.agents;

import org.bg3.forge.toolbox.LibraryService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
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

    @SystemMessage("""
        Analyze the user's request to return a command to execute.

        If the user asks to get all possible values for a given stat attribute, then return "getStatAttributeValues(attributeName)".
        If the user asks to get all possible boost function signatures, then return "getAllBoostFunctionSignatures()".

        Return matches in a semicolon separated list.

        Examples:
        What are examples for the stat attribute DamageType? -> getStatAttributeValues("DamageType")
        Show me stat attribute values for DamageType -> getStatAttributeValues("DamageType")
        Show me boost function signatures -> getAllBoostFunctionSignatures()
        What are some boost functions? -> getAllBoostFunctionSignatures()
        """)
    String jsonCommands(@UserMessage String request);
}
