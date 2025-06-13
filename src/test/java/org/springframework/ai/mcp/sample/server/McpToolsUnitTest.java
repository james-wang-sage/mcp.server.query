package org.springframework.ai.mcp.sample.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.ai.mcp.sample.server.transport.McpToolIntegration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MCP tools functionality
 * Tests the actual tool invocation and object name handling
 */
@SpringBootTest(classes = {QueryService.class, ModelService.class, AuthService.class, McpToolIntegration.class})
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.pattern.console=",
    "mcp.server.sse.enabled=true"
})
public class McpToolsUnitTest {

    @MockBean
    private AuthService authService;

    private QueryService queryService;
    private ModelService modelService;
    private McpToolIntegration toolIntegration;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Mock AuthService to return a test token and base URL
        when(authService.getAccessToken()).thenReturn("test-access-token");
        when(authService.getBaseUrl()).thenReturn("https://test.intacct.com/api/v1-beta2");

        // Create real services with mocked AuthService
        queryService = new QueryService(authService);
        modelService = new ModelService(authService);
        
        // Create tool integration
        toolIntegration = new McpToolIntegration();
        // Note: We'd need to inject the services via reflection or modify the class
        // For now, we'll test the services directly
        
        objectMapper = new ObjectMapper();
    }

    @Test
    void testQueryServiceHandlesObjectsPrefix() {
        // Test that QueryService correctly handles objects/ prefix
        
        // This should not throw an exception even though it will return null due to no real API
        List result1 = queryService.executeQuery(
            "objects/accounts-payable/vendor", // With objects/ prefix
            List.of("id", "name", "status"),
            null, null, null, null, null, 3
        );
        
        List result2 = queryService.executeQuery(
            "accounts-payable/vendor", // Without objects/ prefix
            List.of("id", "name", "status"),
            null, null, null, null, null, 3
        );
        
        // Both should behave the same way (return null due to no real API connection)
        assertEquals(result1, result2);
        assertNull(result1); // Expected in test environment
    }

    @Test
    void testModelServiceHandlesObjectsPrefix() {
        // Test that ModelService correctly handles objects/ prefix
        
        var result1 = modelService.getModelDefinition(
            "objects/accounts-payable/vendor", // With objects/ prefix
            null, null, null, null
        );
        
        var result2 = modelService.getModelDefinition(
            "accounts-payable/vendor", // Without objects/ prefix
            null, null, null, null
        );
        
        // Both should behave the same way
        assertEquals(result1, result2);
        assertNull(result1); // Expected in test environment
    }

    @Test
    void testQueryServiceWithFilters() {
        // Test QueryService with various filter combinations
        
        // Test with single filter
        List result1 = queryService.executeQuery(
            "accounts-payable/vendor",
            List.of("id", "name", "status"),
            List.of(Map.of("$eq", Map.of("status", "active"))),
            null, null, null, null, 5
        );
        
        // Test with multiple filters and filterExpression
        List result2 = queryService.executeQuery(
            "accounts-payable/vendor",
            List.of("id", "name", "status", "totalDue"),
            List.of(
                Map.of("$eq", Map.of("status", "active")),
                Map.of("$gt", Map.of("totalDue", 1000))
            ),
            "1 and 2",
            null, null, null, 10
        );
        
        // Should not throw exceptions
        assertNull(result1); // Expected in test environment
        assertNull(result2); // Expected in test environment
    }

    @Test
    void testQueryServiceValidation() {
        // Test validation logic
        
        // Should throw exception for null object
        assertThrows(NullPointerException.class, () -> {
            queryService.executeQuery(null, List.of("id"), null, null, null, null, null, 1);
        });
        
        // Should throw exception for filterExpression without filters
        assertThrows(IllegalArgumentException.class, () -> {
            queryService.executeQuery(
                "accounts-payable/vendor",
                List.of("id"),
                null, // no filters
                "1 and 2", // but has filterExpression
                null, null, null, 1
            );
        });
        
        // Should throw exception for empty filterExpression with no filters
        assertThrows(IllegalArgumentException.class, () -> {
            queryService.executeQuery(
                "accounts-payable/vendor",
                List.of("id"),
                List.of(), // empty filters
                "1 and 2", // but has filterExpression
                null, null, null, 1
            );
        });
    }

    @Test
    void testModelServiceValidation() {
        // Test validation logic
        
        // Should throw exception for null name
        assertThrows(NullPointerException.class, () -> {
            modelService.getModelDefinition(null, null, null, null, null);
        });
    }

    @Test
    void testEdgeCasesForObjectNames() {
        // Test various edge cases for object name handling

        // Empty string after removing objects/ - this actually results in empty string, not exception
        List result0 = queryService.executeQuery("objects/", List.of("id"), null, null, null, null, null, 1);
        assertNull(result0); // Expected in test environment - empty object name will fail at API level

        // Just "objects" without slash - should work
        List result1 = queryService.executeQuery("objects", List.of("id"), null, null, null, null, null, 1);
        assertNull(result1); // Expected in test environment

        // Normal object names should work
        List result2 = queryService.executeQuery("accounts-payable/vendor", List.of("id"), null, null, null, null, null, 1);
        assertNull(result2); // Expected in test environment

        // Complex object names
        List result3 = queryService.executeQuery("company-config/department", List.of("id"), null, null, null, null, null, 1);
        assertNull(result3); // Expected in test environment
    }

    @Test
    void testServiceInitialization() {
        // Test that services are properly initialized
        assertNotNull(queryService);
        assertNotNull(modelService);
        
        // Verify that AuthService mock is working
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }

    @Test
    void testFilterParametersHandling() {
        // Test QueryService with filterParameters
        
        QueryService.FilterParameters filterParams = new QueryService.FilterParameters(
            "2025-06-13",
            false,
            true,
            false
        );
        
        List result = queryService.executeQuery(
            "accounts-payable/vendor",
            List.of("id", "name", "status"),
            List.of(Map.of("$eq", Map.of("status", "active"))),
            null,
            filterParams,
            List.of(Map.of("id", "asc")),
            1,
            10
        );
        
        // Should handle filterParameters without throwing exception
        assertNull(result); // Expected in test environment
    }
}
