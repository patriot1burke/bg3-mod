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
        Answer user questions in natural langauge using the data provided above.
        List an items found in the data.

        """)
    String answer(@UserMessage String question, String json);

    @SystemMessage("""
        Your task answer questions about JSON data obtained from a Baldur's Gate 3 item database.

        Input:
        {json}

        Output:
        Any item name mentioned should be wrapped in an HTML anchor with an anchor attribute called "data-tooltip".  The value of "data-tooltip" should be a json string that sets a "title" value to the name of the item and sets the "content" value to be the boostDescription of the item and sets the "footer" value to the the description of the item.
        """)
    String list(@UserMessage String question, String json);

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
