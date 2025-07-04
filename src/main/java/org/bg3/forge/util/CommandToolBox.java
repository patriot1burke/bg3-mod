package org.bg3.forge.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class CommandToolBox {


    private List<ToolSpecification> toolSpecifications = new ArrayList<>();
    
    private Map<String, ToolExecutor> toolExecutors = new HashMap<>();
    private Map<String, Boolean> isDataTool = new HashMap<>();
    private static final String SYSTEM_MESSAGE = """
            Analyze the following user request and forward the request to the appropriate tool.
                Finally return the answer that you received without any modification.
            """;
    private final ChatModel chat;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CommandToolBox(ChatModel chat) {
        this.chat = chat;
    }


    public void addToolbox(Object target) {
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                toolSpecifications.add(ToolSpecifications.toolSpecificationFrom(method));
                ToolExecutor toolExecutor = new DefaultToolExecutor(target, method);
                toolExecutors.put(method.getName(), toolExecutor);
                if (method.getReturnType().equals(String.class)) {
                    isDataTool.put(method.getName(), false);
                } else {
                    isDataTool.put(method.getName(), true);
                }
            }
        }
    }

    public String execute(String command) throws Exception {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(SystemMessage.from(SYSTEM_MESSAGE), UserMessage.from(command))
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse response = chat.chat(chatRequest);
        AiMessage aiMessage = response.aiMessage();
        if (!aiMessage.hasToolExecutionRequests()) {
            return "";
        }
        String msg = "[";
        boolean first = true;
        for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
            String toolName = toolExecutionRequest.name();
            ToolExecutor toolExecutor = toolExecutors.get(toolName);
            if (toolExecutor == null) {
                throw new Exception("Tool executor not found for tool: " + toolName);
            }
            String toolResponse = toolExecutor.execute(toolExecutionRequest, null);
            
            boolean isData = isDataTool.get(toolName);
            if (!isData) {
                toolResponse = objectMapper.writeValueAsString(toolResponse);
            }
            String responseString = "{\"command\":\"" + toolName + "\",\"response\":" + toolResponse + "}";
            if (first) {
                msg += responseString;
                first = false;
            } else {
                msg += "," + responseString;
            }
        }

        return msg + "]";

    }

}
