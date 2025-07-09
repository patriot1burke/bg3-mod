package org.baldurs.forge.nli;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkus.logging.Log;

public class ToolBoxNLIInvoker {


    private List<ToolSpecification> toolSpecifications = new ArrayList<>();
    
    private Map<String, ToolBoxNLIExecutor> toolExecutors = new HashMap<>();
    private Map<String, Boolean> isDataTool = new HashMap<>();
    private static final String SYSTEM_MESSAGE = """
            Analyze the following user request and forward the request to the appropriate tool.
                Finally return the answer that you received without any modification.
            """;

    private static final SystemMessage DEFAULT_SYSTEM_MESSAGE = SystemMessage.from(SYSTEM_MESSAGE);
    private final ChatModel chat;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private SystemMessage systemMessage = DEFAULT_SYSTEM_MESSAGE;

    public ToolBoxNLIInvoker(ChatModel chat) {
        this.chat = chat;
    }

    public void addToolSpecifications(List<ToolSpecification> toolSpecifications) {
        this.toolSpecifications.addAll(toolSpecifications);
    }

    /**
     * Set the system message for the tool box.
     * @param systemMessage non null
     */
    public void systemMessage(SystemMessage systemMessage) {
        Objects.requireNonNull(systemMessage, "systemMessage");
        this.systemMessage = systemMessage;
    }

    public void addToolbox(Object toolBox, Class<?> toolClass) {
        Method[] methods = toolClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                Tool tool = method.getAnnotation(Tool.class);
                String toolName = tool.name().isEmpty() ? method.getName() : method.getName();
                Method methodToUse = method;
                if (toolClass != toolBox.getClass()) {
                    try {
                        methodToUse = toolBox.getClass().getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException e) {
                        throw new IllegalArgumentException("Method not found in tool box class: " + method.getName(), e);
                    }
                }
                ToolBoxNLIExecutor toolExecutor = new ToolBoxNLIExecutor(toolBox, method, methodToUse);
                toolExecutors.put(toolName, toolExecutor);
                if (method.getReturnType().equals(String.class)) {
                    isDataTool.put(toolName, false);
                } else {
                    isDataTool.put(toolName, true);
                }
            }
        }
    }

    public String execute(String userMessage) throws Exception {
        AiMessage aiMessage = chat(userMessage);
        if (!aiMessage.hasToolExecutionRequests()) {
            return "";
        }
        String msg = "[";
        boolean first = true;
        for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
            String toolName = toolExecutionRequest.name();
            ToolBoxNLIExecutor toolExecutor = toolExecutors.get(toolName);
            if (toolExecutor == null) {
                throw new Exception("Tool executor not found for tool: " + toolName);
            }
            String toolResponse = toolExecutor.execute(toolExecutionRequest, null);
            
            boolean isData = isDataTool.get(toolName);
            if (!isData) {
                toolResponse = objectMapper.writeValueAsString(toolResponse);
            }
            String responseString = "{\"tool\":\"" + toolName + "\",\"response\":" + toolResponse + "}";
            if (first) {
                msg += responseString;
                first = false;
            } else {
                msg += "," + responseString;
            }
        }

        return msg + "]";

    }

    public List<ToolResponse> invoke(String userMessage) throws Throwable {
        AiMessage aiMessage = chat(userMessage);
        if (!aiMessage.hasToolExecutionRequests()) {
            return Collections.emptyList();
        }
        List<ToolResponse> toolResponses = new ArrayList<>();
        for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
            String toolName = toolExecutionRequest.name();
            ToolBoxNLIExecutor toolExecutor = toolExecutors.get(toolName);
            if (toolExecutor == null) {
                throw new Exception("Tool executor not found for tool: " + toolName);
            }
            Object toolResponse = toolExecutor.invoke(toolExecutionRequest, null);
            toolResponses.add(new ToolResponse(toolName, toolResponse));
        }

        return toolResponses;

    }

    

    protected AiMessage chat(String userMessage) throws Exception {
        Log.info("Sending chat prompt: " + userMessage);
        Log.info("Tool specifications size: " + toolSpecifications.size());
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(systemMessage, UserMessage.from(userMessage))
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse response = chat.chat(chatRequest);
        return response.aiMessage();
    }

}
