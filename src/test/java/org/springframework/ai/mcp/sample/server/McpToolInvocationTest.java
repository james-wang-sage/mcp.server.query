package org.springframework.ai.mcp.sample.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.transport.McpToolIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test MCP tool invocation through the integration layer
 * This simulates how Claude would call the MCP tools
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.pattern.console=",
    "mcp.server.sse.enabled=true"
})
public class McpToolInvocationTest {

    @Autowired
    private McpToolIntegration toolIntegration;

    @MockBean
    private AuthService authService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Mock AuthService to return test values
        when(authService.getAccessToken()).thenReturn("test-access-token");
        when(authService.getBaseUrl()).thenReturn("https://test.intacct.com/api/v1-beta2");
        
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetAvailableTools() {
        // Test that we can get the list of available tools
        List<Map<String, Object>> tools = toolIntegration.getAvailableTools();
        
        assertNotNull(tools);
        assertFalse(tools.isEmpty());
        
        // Should have tools from both QueryService and ModelService
        boolean hasQueryTools = tools.stream()
            .anyMatch(tool -> tool.get("name").toString().contains("executeQuery"));
        boolean hasModelTools = tools.stream()
            .anyMatch(tool -> tool.get("name").toString().contains("getModelDefinition"));
        
        assertTrue(hasQueryTools, "Should have query tools");
        assertTrue(hasModelTools, "Should have model tools");
        
        // Print available tools for debugging
        System.out.println("Available tools:");
        tools.forEach(tool -> 
            System.out.println("  - " + tool.get("name") + ": " + tool.get("description"))
        );
    }

    @Test
    void testInvokeExecuteQueryTool() throws Exception {
        // Test invoking the executeQuery tool with objects/ prefix
        JsonNode arguments = objectMapper.readTree("""
            {
                "object": "objects/accounts-payable/vendor",
                "fields": ["id", "name", "status"],
                "size": 3
            }
            """);

        Map<String, Object> result = toolIntegration.invokeTool("query_executeQuery", arguments);
        
        assertNotNull(result);
        
        // In test environment, we expect null result due to no real API connection
        // But the tool should execute without throwing exceptions
        System.out.println("Query tool result: " + result);
    }

    @Test
    void testInvokeGetModelDefinitionTool() throws Exception {
        // Test invoking the getModelDefinition tool with objects/ prefix
        JsonNode arguments = objectMapper.readTree("""
            {
                "name": "objects/accounts-payable/vendor"
            }
            """);

        Map<String, Object> result = toolIntegration.invokeTool("model_getModelDefinition", arguments);
        
        assertNotNull(result);
        
        // In test environment, we expect null result due to no real API connection
        System.out.println("Model tool result: " + result);
    }

    @Test
    void testInvokeQueryToolWithFilters() throws Exception {
        // Test invoking executeQuery with complex filters
        JsonNode arguments = objectMapper.readTree("""
            {
                "object": "accounts-payable/vendor",
                "fields": ["id", "name", "status", "totalDue"],
                "filters": [
                    {"$eq": {"status": "active"}},
                    {"$gt": {"totalDue": 1000}}
                ],
                "filterExpression": "1 and 2",
                "orderBy": [{"id": "asc"}],
                "size": 10
            }
            """);

        Map<String, Object> result = toolIntegration.invokeTool("query_executeQuery", arguments);
        
        assertNotNull(result);
        System.out.println("Complex query result: " + result);
    }

    @Test
    void testInvokeQueryToolWithFilterParameters() throws Exception {
        // Test invoking executeQuery with filterParameters
        JsonNode arguments = objectMapper.readTree("""
            {
                "object": "accounts-payable/vendor",
                "fields": ["id", "name", "status"],
                "filterParameters": {
                    "asOfDate": "2025-06-13",
                    "caseSensitiveComparison": true,
                    "includeHierarchyFields": false,
                    "includePrivate": false
                },
                "size": 5
            }
            """);

        Map<String, Object> result = toolIntegration.invokeTool("query_executeQuery", arguments);
        
        assertNotNull(result);
        System.out.println("Query with filterParameters result: " + result);
    }

    @Test
    void testInvokeListAvailableModelsTool() throws Exception {
        // Test invoking the listAvailableModels tool
        JsonNode arguments = objectMapper.createObjectNode(); // Empty arguments

        Map<String, Object> result = toolIntegration.invokeTool("model_listAvailableModels", arguments);
        
        assertNotNull(result);
        System.out.println("List models result: " + result);
    }

    @Test
    void testToolErrorHandling() throws Exception {
        // Test error handling for invalid tool names
        JsonNode arguments = objectMapper.createObjectNode();

        Map<String, Object> result = toolIntegration.invokeTool("invalid_tool", arguments);
        
        assertNotNull(result);
        assertTrue((Boolean) result.get("error"), "Should return error for invalid tool");
        assertNotNull(result.get("message"));
        
        System.out.println("Error result: " + result);
    }

    @Test
    void testToolValidationErrors() throws Exception {
        // Test validation errors (null object)
        JsonNode arguments = objectMapper.readTree("""
            {
                "object": null,
                "fields": ["id"]
            }
            """);

        Map<String, Object> result = toolIntegration.invokeTool("query_executeQuery", arguments);
        
        assertNotNull(result);
        // Should handle the validation error gracefully
        System.out.println("Validation error result: " + result);
    }

    @Test
    void testGetToolInfo() {
        // Test getting information about a specific tool
        Map<String, Object> toolInfo = toolIntegration.getToolInfo("query_executeQuery");
        
        assertNotNull(toolInfo);
        assertEquals("query_executeQuery", toolInfo.get("name"));
        assertNotNull(toolInfo.get("description"));
        
        System.out.println("Tool info: " + toolInfo);
    }

    @Test
    void testGetNonExistentToolInfo() {
        // Test getting information about a non-existent tool
        Map<String, Object> toolInfo = toolIntegration.getToolInfo("nonexistent_tool");
        
        assertNull(toolInfo);
    }

    @Test
    void testMockVerification() {
        // Verify that our mocks are being called
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }
}
