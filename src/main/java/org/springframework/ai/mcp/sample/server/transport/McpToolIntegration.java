package org.springframework.ai.mcp.sample.server.transport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.ModelService;
import org.springframework.ai.mcp.sample.server.QueryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integrates MCP tools with SSE transport by providing tool discovery and invocation.
 * This service bridges the gap between the HTTP-based SSE transport and the 
 * annotation-based tool system.
 */
@Service
@ConditionalOnProperty(name = "mcp.server.sse.enabled", havingValue = "true")
public class McpToolIntegration {

    private static final Logger logger = LoggerFactory.getLogger(McpToolIntegration.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ModelService modelService;

    @Autowired
    private QueryService queryService;

    /**
     * Get all available tools from both services
     */
    public List<Map<String, Object>> getAvailableTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        // Add tools from ModelService
        tools.addAll(getToolsFromService(modelService, "model"));
        
        // Add tools from QueryService
        tools.addAll(getToolsFromService(queryService, "query"));
        
        logger.debug("Found {} total tools", tools.size());
        return tools;
    }

    /**
     * Invoke a tool by name with the given arguments
     */
    public Map<String, Object> invokeTool(String toolName, JsonNode arguments) throws Exception {
        logger.debug("Invoking tool: {} with arguments: {}", toolName, arguments);

        try {
            if (toolName.startsWith("model_")) {
                return invokeServiceTool(modelService, toolName, arguments);
            } else if (toolName.startsWith("query_")) {
                return invokeServiceTool(queryService, toolName, arguments);
            } else {
                throw new IllegalArgumentException("Unknown tool prefix: " + toolName);
            }
        } catch (Exception e) {
            logger.error("Error invoking tool: {}", toolName, e);
            return Map.of(
                "error", true,
                "message", e.getMessage(),
                "tool", toolName
            );
        }
    }

    /**
     * Get tool information for a specific tool
     */
    public Map<String, Object> getToolInfo(String toolName) {
        List<Map<String, Object>> allTools = getAvailableTools();
        return allTools.stream()
            .filter(tool -> toolName.equals(tool.get("name")))
            .findFirst()
            .orElse(null);
    }

    /**
     * Extract tools from a service class using reflection
     */
    private List<Map<String, Object>> getToolsFromService(Object service, String prefix) {
        List<Map<String, Object>> tools = new ArrayList<>();
        Class<?> serviceClass = service.getClass();

        for (Method method : serviceClass.getDeclaredMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                Map<String, Object> toolInfo = createToolInfo(method, toolAnnotation, prefix);
                tools.add(toolInfo);
            }
        }

        logger.debug("Found {} tools in {}", tools.size(), serviceClass.getSimpleName());
        return tools;
    }

    /**
     * Create tool information map from method and annotation
     */
    private Map<String, Object> createToolInfo(Method method, Tool annotation, String prefix) {
        String toolName = prefix + "_" + method.getName();
        
        Map<String, Object> toolInfo = new HashMap<>();
        toolInfo.put("name", toolName);
        toolInfo.put("description", annotation.description());
        toolInfo.put("method", method.getName());
        toolInfo.put("service", prefix);
        
        // Extract parameter information
        List<Map<String, Object>> parameters = new ArrayList<>();
        Class<?>[] paramTypes = method.getParameterTypes();
        String[] paramNames = getParameterNames(method);
        
        for (int i = 0; i < paramTypes.length; i++) {
            Map<String, Object> param = new HashMap<>();
            param.put("name", paramNames[i]);
            param.put("type", paramTypes[i].getSimpleName());
            param.put("required", true); // Assume all parameters are required for now
            parameters.add(param);
        }
        
        toolInfo.put("parameters", parameters);
        toolInfo.put("returnType", method.getReturnType().getSimpleName());
        
        return toolInfo;
    }

    /**
     * Get parameter names for a method (simplified version)
     */
    private String[] getParameterNames(Method method) {
        // In a real implementation, you might use parameter name discovery
        // For now, generate generic names
        int paramCount = method.getParameterCount();
        String[] names = new String[paramCount];
        for (int i = 0; i < paramCount; i++) {
            names[i] = "param" + i;
        }
        return names;
    }

    /**
     * Invoke a tool method on a service
     */
    private Map<String, Object> invokeServiceTool(Object service, String toolName, JsonNode arguments) throws Exception {
        String methodName = extractMethodName(toolName);
        Method method = findToolMethod(service.getClass(), methodName);
        
        if (method == null) {
            throw new IllegalArgumentException("Tool method not found: " + methodName);
        }

        // Convert arguments to method parameters
        Object[] params = convertArguments(method, arguments);
        
        // Invoke the method
        Object result = method.invoke(service, params);
        
        // Return structured response
        return Map.of(
            "success", true,
            "result", result != null ? result : "null",
            "tool", toolName
        );
    }

    /**
     * Extract method name from tool name (remove prefix)
     */
    private String extractMethodName(String toolName) {
        int underscoreIndex = toolName.indexOf('_');
        if (underscoreIndex >= 0 && underscoreIndex < toolName.length() - 1) {
            return toolName.substring(underscoreIndex + 1);
        }
        return toolName;
    }

    /**
     * Find a method with @Tool annotation by name
     */
    private Method findToolMethod(Class<?> serviceClass, String methodName) {
        for (Method method : serviceClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class) && method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Convert JSON arguments to method parameters
     */
    private Object[] convertArguments(Method method, JsonNode arguments) throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        
        if (arguments == null || arguments.isNull()) {
            // No arguments provided, use defaults or nulls
            for (int i = 0; i < params.length; i++) {
                params[i] = getDefaultValue(paramTypes[i]);
            }
            return params;
        }

        // Convert arguments based on parameter types
        if (arguments.isObject()) {
            // Arguments provided as object with named parameters
            String[] paramNames = getParameterNames(method);
            for (int i = 0; i < paramTypes.length; i++) {
                JsonNode argValue = arguments.get(paramNames[i]);
                params[i] = convertJsonToType(argValue, paramTypes[i]);
            }
        } else if (arguments.isArray()) {
            // Arguments provided as array
            for (int i = 0; i < paramTypes.length && i < arguments.size(); i++) {
                JsonNode argValue = arguments.get(i);
                params[i] = convertJsonToType(argValue, paramTypes[i]);
            }
        } else {
            // Single argument
            if (paramTypes.length > 0) {
                params[0] = convertJsonToType(arguments, paramTypes[0]);
            }
        }

        return params;
    }

    /**
     * Convert JSON node to specific Java type
     */
    private Object convertJsonToType(JsonNode jsonNode, Class<?> targetType) throws Exception {
        if (jsonNode == null || jsonNode.isNull()) {
            return getDefaultValue(targetType);
        }

        if (targetType == String.class) {
            return jsonNode.asText();
        } else if (targetType == int.class || targetType == Integer.class) {
            return jsonNode.asInt();
        } else if (targetType == long.class || targetType == Long.class) {
            return jsonNode.asLong();
        } else if (targetType == double.class || targetType == Double.class) {
            return jsonNode.asDouble();
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return jsonNode.asBoolean();
        } else {
            // For complex types, try to deserialize from JSON
            return objectMapper.treeToValue(jsonNode, targetType);
        }
    }

    /**
     * Get default value for a type
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        return null; // For reference types
    }

    /**
     * Get available resources (placeholder implementation)
     */
    public List<Map<String, Object>> getAvailableResources() {
        // This would be implemented based on actual resource providers
        return List.of(
            Map.of(
                "uri", "resource://models",
                "name", "Available Models",
                "description", "List of available AI models"
            ),
            Map.of(
                "uri", "resource://queries",
                "name", "Query Templates",
                "description", "Available query templates"
            )
        );
    }

    /**
     * Get available prompts (placeholder implementation)
     */
    public List<Map<String, Object>> getAvailablePrompts() {
        // This would be implemented based on actual prompt providers
        return List.of(
            Map.of(
                "name", "system_prompt",
                "description", "System prompt for AI interactions",
                "arguments", List.of(
                    Map.of("name", "context", "type", "string", "required", false)
                )
            )
        );
    }
}