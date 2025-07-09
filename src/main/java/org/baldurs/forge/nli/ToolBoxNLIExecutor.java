package org.baldurs.forge.nli;
import static dev.langchain4j.internal.Utils.isNullOrBlank;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.internal.Json;
import dev.langchain4j.service.tool.ToolExecutor;
import io.quarkus.logging.Log;


public class ToolBoxNLIExecutor implements ToolExecutor {

    private final Object object;
    private final Method originalMethod;
    private final Method methodToInvoke;

    public ToolBoxNLIExecutor(Object object, Method method) {
        this.object = Objects.requireNonNull(object, "object");
        this.originalMethod = Objects.requireNonNull(method, "method");
        this.methodToInvoke = this.originalMethod;
    }

    public ToolBoxNLIExecutor(Object object, ToolExecutionRequest toolExecutionRequest) {
        this.object = Objects.requireNonNull(object, "object");
        Objects.requireNonNull(toolExecutionRequest, "toolExecutionRequest");
        this.originalMethod = findMethod(object, toolExecutionRequest);
        this.methodToInvoke = this.originalMethod;
    }

    private Method findMethod(Object object, ToolExecutionRequest toolExecutionRequest) {
        String requestedMethodName = toolExecutionRequest.name();

        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.getName().equals(requestedMethodName)) {
                return method;
            }
        }

        throw new IllegalArgumentException(String.format(
                "Method '%s' is not found in object '%s'",
                requestedMethodName, object.getClass().getName()));
    }

    /**
     * When methods annotated with @Tool are wrapped into proxies (AOP),
     * the parameters of the proxied method do not retain their original names.
     * Therefore, access to the original method is required to retrieve those names.
     *
     * @param object         the object on which the method should be invoked
     * @param originalMethod the original method, used to retrieve parameter names and prepare arguments
     * @param methodToInvoke the method that should actually be invoked
     */
    public ToolBoxNLIExecutor(Object object, Method originalMethod, Method methodToInvoke) {
        this.object = Objects.requireNonNull(object, "object");
        this.originalMethod = Objects.requireNonNull(originalMethod, "originalMethod");
        this.methodToInvoke = Objects.requireNonNull(methodToInvoke, "methodToInvoke");
    }

    public Object invoke(ToolExecutionRequest toolExecutionRequest, Object memoryId) throws Throwable {
        Map<String, Object> argumentsMap = argumentsAsMap(toolExecutionRequest.arguments());
        Object[] arguments = prepareArguments(originalMethod, argumentsMap, memoryId);
        methodToInvoke.setAccessible(true);
        try {
            return methodToInvoke.invoke(object, arguments);
        } catch (IllegalAccessException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Log.error("Error invoking tool: " + toolExecutionRequest.name(), e.getTargetException());
            throw e.getTargetException();
        }
    }


    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {

        Map<String, Object> argumentsMap = argumentsAsMap(toolExecutionRequest.arguments());
        Object[] arguments = prepareArguments(originalMethod, argumentsMap, memoryId);
        try {
            return execute(arguments);
        } catch (IllegalAccessException e) {
            try {
                methodToInvoke.setAccessible(true);
                return execute(arguments);
            } catch (IllegalAccessException e2) {
                throw new RuntimeException(e2);
            } catch (InvocationTargetException e2) {
                return e2.getCause().getMessage();
            }
        } catch (InvocationTargetException e) {
            return  e.getCause().getMessage();
        }
    }

    private String execute(Object[] arguments) throws IllegalAccessException, InvocationTargetException {
        Object result = methodToInvoke.invoke(object, arguments);
        Class<?> returnType = methodToInvoke.getReturnType();
        if (returnType == void.class) {
            return "Success";
        } else if (returnType == String.class) {
            return (String) result;
        } else {
            return Json.toJson(result);
        }
    }

    static Object[] prepareArguments(Method method, Map<String, Object> argumentsMap, Object memoryId) {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {

            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(ToolMemoryId.class)) {
                arguments[i] = memoryId;
                continue;
            }

            String parameterName = parameter.getName();
            if (argumentsMap.containsKey(parameterName)) {
                Object argument = argumentsMap.get(parameterName);
                Class<?> parameterClass = parameter.getType();
                Type parameterType = parameter.getParameterizedType();

                arguments[i] = coerceArgument(argument, parameterName, parameterClass, parameterType);
            }
        }

        return arguments;
    }

    static Object coerceArgument(Object argument, String parameterName, Class<?> parameterClass, Type parameterType) {
        if (parameterClass == String.class) {
            return argument.toString();
        }

        if (parameterClass.isEnum()) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Class<Enum> enumClass = (Class<Enum>) parameterClass;
                try {
                    return Enum.valueOf(
                            enumClass, Objects.requireNonNull(argument).toString());
                } catch (IllegalArgumentException e) {
                    // try to convert to uppercase as a last resort
                    return Enum.valueOf(
                            enumClass,
                            Objects.requireNonNull(argument).toString().toUpperCase());
                }
            } catch (Exception | Error e) {
                throw new IllegalArgumentException(
                        String.format(
                                "Argument \"%s\" is not a valid enum value for %s: <%s>",
                                parameterName, parameterClass.getName(), argument),
                        e);
            }
        }

        if (parameterClass == Boolean.class || parameterClass == boolean.class) {
            if (argument instanceof Boolean) {
                return argument;
            }
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is not convertable to %s, got %s: <%s>",
                    parameterName, parameterClass.getName(), argument.getClass().getName(), argument));
        }

        if (parameterClass == Double.class || parameterClass == double.class) {
            return getDoubleValue(argument, parameterName, parameterClass);
        }

        if (parameterClass == Float.class || parameterClass == float.class) {
            double doubleValue = getDoubleValue(argument, parameterName, parameterClass);
            checkBounds(doubleValue, parameterName, parameterClass, -Float.MIN_VALUE, Float.MAX_VALUE);
            return (float) doubleValue;
        }

        if (parameterClass == BigDecimal.class) {
            return BigDecimal.valueOf(getDoubleValue(argument, parameterName, parameterClass));
        }

        if (parameterClass == Integer.class || parameterClass == int.class) {
            return (int)
                    getBoundedLongValue(argument, parameterName, parameterClass, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        if (parameterClass == Long.class || parameterClass == long.class) {
            return getBoundedLongValue(argument, parameterName, parameterClass, Long.MIN_VALUE, Long.MAX_VALUE);
        }

        if (parameterClass == Short.class || parameterClass == short.class) {
            return (short)
                    getBoundedLongValue(argument, parameterName, parameterClass, Short.MIN_VALUE, Short.MAX_VALUE);
        }

        if (parameterClass == Byte.class || parameterClass == byte.class) {
            return (byte) getBoundedLongValue(argument, parameterName, parameterClass, Byte.MIN_VALUE, Byte.MAX_VALUE);
        }

        if (parameterClass == BigInteger.class) {
            return BigDecimal.valueOf(getNonFractionalDoubleValue(argument, parameterName, parameterClass))
                    .toBigInteger();
        }

        if (Collection.class.isAssignableFrom(parameterClass) || Map.class.isAssignableFrom(parameterClass)) {
            // Conversion to JSON and back is required when parameterType is a POJO
            return Json.fromJson(Json.toJson(argument), parameterType);
        }

        if (parameterClass == UUID.class) {
            return UUID.fromString(argument.toString());
        }

        if (argument instanceof String) {
            return Json.fromJson(argument.toString(), parameterClass);
        } else {
            // Conversion to JSON and back is required when parameterClass is a POJO
            return Json.fromJson(Json.toJson(argument), parameterClass);
        }
    }

    private static double getDoubleValue(Object argument, String parameterName, Class<?> parameterType) {
        if (argument instanceof String) {
            try {
                return Double.parseDouble(argument.toString());
            } catch (Exception e) {
                // nothing, will be handled with bellow code
            }
        }
        if (!(argument instanceof Number)) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is not convertable to %s, got %s: <%s>",
                    parameterName, parameterType.getName(), argument.getClass().getName(), argument));
        }
        return ((Number) argument).doubleValue();
    }

    private static double getNonFractionalDoubleValue(Object argument, String parameterName, Class<?> parameterType) {
        double doubleValue = getDoubleValue(argument, parameterName, parameterType);
        if (!hasNoFractionalPart(doubleValue)) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" has non-integer value for %s: <%s>",
                    parameterName, parameterType.getName(), argument));
        }
        return doubleValue;
    }

    private static void checkBounds(
            double doubleValue, String parameterName, Class<?> parameterType, double minValue, double maxValue) {
        if (doubleValue < minValue || doubleValue > maxValue) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is out of range for %s: <%s>",
                    parameterName, parameterType.getName(), doubleValue));
        }
    }

    public static long getBoundedLongValue(
            Object argument, String parameterName, Class<?> parameterType, long minValue, long maxValue) {
        double doubleValue = getNonFractionalDoubleValue(argument, parameterName, parameterType);
        checkBounds(doubleValue, parameterName, parameterType, minValue, maxValue);
        return (long) doubleValue;
    }

    static boolean hasNoFractionalPart(Double doubleValue) {
        return doubleValue.equals(Math.floor(doubleValue));
    }

    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");

    private static final Pattern LEADING_TRAILING_QUOTE_PATTERN = Pattern.compile("^\"|\"$");

    private static final Pattern ESCAPED_QUOTE_PATTERN = Pattern.compile("\\\\\"");

    private static final Type MAP_TYPE = new ParameterizedType() {

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class, Object.class};
        }

        @Override
        public Type getRawType() {
            return Map.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };

    /**
     * Convert arguments to map.
     *
     * @param arguments json string
     * @return map
     */
    static Map<String, Object> argumentsAsMap(String arguments) {
        if (isNullOrBlank(arguments)) {
            return Map.of();
        }

        String normalizeArguments = normalizeJsonString(arguments);
        return Json.fromJson(removeTrailingComma(normalizeArguments), MAP_TYPE);
    }

    /**
     * Removes trailing commas before closing braces or brackets in JSON strings.
     *
     * @param json the JSON string
     * @return the corrected JSON string
     */
    static String removeTrailingComma(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Matcher matcher = TRAILING_COMMA_PATTERN.matcher(json);
        return matcher.replaceAll("$1");
    }

    /**
     * Normalizes a JSON string by removing leading and trailing quotes and unescaping internal double quotes.
     *
     * @param arguments the raw JSON string
     * @return the normalized JSON string
     */
    static String normalizeJsonString(String arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return arguments;
        }

        Matcher leadingTrailingMatcher = LEADING_TRAILING_QUOTE_PATTERN.matcher(arguments);
        String normalizedJson = leadingTrailingMatcher.replaceAll("");

        Matcher escapedQuoteMatcher = ESCAPED_QUOTE_PATTERN.matcher(normalizedJson);
        return escapedQuoteMatcher.replaceAll("\"");
    }

}
