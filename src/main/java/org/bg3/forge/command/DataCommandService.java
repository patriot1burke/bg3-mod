package org.bg3.forge.command;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bg3.forge.toolbox.LibraryService;
import org.bg3.forge.util.CommandToolBox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataCommandService {

    private CommandToolBox commandToolBox;

    @Inject
    ChatModel chat;

    @Inject
    LibraryService library;

    public void start(@Observes StartupEvent event) throws Exception {
        commandToolBox = new CommandToolBox(chat);
        commandToolBox.addToolbox(this);

    }

    public String execute(String command) throws Exception {
        return commandToolBox.execute(command);
    }

    @Tool("Get all possible values for a Stat attribute")
    public List<String> getStatAttributes(String statAttribute) throws Exception {
        Log.info("COMMAND getStatAttributes: " + statAttribute);
        return library.getStatAttributeValues(statAttribute);
    }

    @Tool("get all possible boost function signatures")
    public List<String> getBoostFunctions() throws Exception {
        Log.info("COMMAND getBoostFunctions");
        return library.getAllBoostFunctionSignatures();
    }
}
