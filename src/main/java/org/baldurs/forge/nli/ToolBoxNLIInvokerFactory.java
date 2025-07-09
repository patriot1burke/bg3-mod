package org.baldurs.forge.nli;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.ChatModel;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

@ApplicationScoped
public class ToolBoxNLIInvokerFactory {
    @Inject
    private ChatModel chat;
    
    @Inject
    private BeanManager beanManager;

    Map<Class<?>, List<ToolSpecification>> toolNLIInvokers = new ConcurrentHashMap<>();

    @Produces
    public ToolBoxNLIInvoker createToolNLIInvoker(InjectionPoint injectionPoint) {
        //Log.info("Creating ToolNLIInvoker******");
        ToolBoxNLI toolBoxNLI = injectionPoint.getAnnotated().getAnnotation(ToolBoxNLI.class);
        if (toolBoxNLI == null) {
            throw new IllegalArgumentException("ToolBoxNLI annotation not found");
        }
        Class<?>[] toolClasses = toolBoxNLI.value();
        ToolBoxNLIInvoker toolNLIInvoker = new ToolBoxNLIInvoker(chat);
        for (Class<?> toolClass : toolClasses) {
            List<ToolSpecification> toolSpecifications = getToolSpecifications(toolClass);
            if (toolSpecifications.isEmpty()) {
                Log.debug("No tool specifications found for tool class: " + toolClass.getName());
                continue;
            }
            toolNLIInvoker.addToolSpecifications(toolSpecifications);

            Bean<?> bean = beanManager.resolve(beanManager.getBeans(toolClass));
            if (bean != null) {
                Object toolInstance = beanManager.getReference(bean, toolClass, beanManager.createCreationalContext(bean));
                toolNLIInvoker.addToolbox(toolInstance, toolClass);
            } else {
                throw new IllegalArgumentException("Bean not found for tool class: " + toolClass.getName());
            }
        }
        return toolNLIInvoker;
    }

    protected List<ToolSpecification> getToolSpecifications(Class<?> toolClass) {
        return toolNLIInvokers.computeIfAbsent(toolClass, this::createToolSpecifications);
    }

    protected List<ToolSpecification> createToolSpecifications(Class<?> toolClass) {
        List<ToolSpecification> toolSpecifications = new ArrayList<>();
        for (Method method : toolClass.getMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                toolSpecifications.add(ToolSpecifications.toolSpecificationFrom(method));
            }
        }
        return toolSpecifications;
    }
}
