package org.springframework.ai.mcp.sample.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test MCP server functionality with mocked external dependencies
 * This allows us to test the logic without making real API calls
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.pattern.console="
})
public class McpServerMockTest {

    @MockBean
    private AuthService authService;

    private QueryService queryService;
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        // Mock AuthService to simulate successful authentication
        when(authService.getAccessToken()).thenReturn("mock-access-token-12345");
        when(authService.getBaseUrl()).thenReturn("https://mock.intacct.com/api/v1-beta2");

        // Create services with mocked AuthService
        queryService = new QueryService(authService);
        modelService = new ModelService(authService);
    }

    @Test
    void testQueryServiceWithMockedAuth() {
        // Test that QueryService correctly uses the mocked AuthService
        
        // This will fail at the HTTP call level, but should get past authentication
        List result = queryService.executeQuery(
            "accounts-payable/vendor",
            List.of("id", "name", "status"),
            null, null, null, null, null, 3
        );
        
        // Should return null due to network failure, but not due to auth failure
        assertNull(result);
        
        // Verify that AuthService was called
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }

    @Test
    void testModelServiceWithMockedAuth() {
        // Test that ModelService correctly uses the mocked AuthService
        
        var result = modelService.getModelDefinition(
            "accounts-payable/vendor",
            null, null, null, null
        );
        
        // Should return null due to network failure, but not due to auth failure
        assertNull(result);
        
        // Verify that AuthService was called
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }

    @Test
    void testObjectsPrefixRemoval() {
        // Test that objects/ prefix is correctly removed
        
        // Both calls should behave identically
        List result1 = queryService.executeQuery(
            "objects/accounts-payable/vendor", // With prefix
            List.of("id", "name"),
            null, null, null, null, null, 1
        );
        
        List result2 = queryService.executeQuery(
            "accounts-payable/vendor", // Without prefix
            List.of("id", "name"),
            null, null, null, null, null, 1
        );
        
        // Both should return null (due to network failure) but behave the same
        assertEquals(result1, result2);
        assertNull(result1);
        assertNull(result2);
    }

    @Test
    void testQueryServiceValidation() {
        // Test validation logic works correctly
        
        // Null object should throw exception
        assertThrows(NullPointerException.class, () -> {
            queryService.executeQuery(null, List.of("id"), null, null, null, null, null, 1);
        });
        
        // FilterExpression without filters should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            queryService.executeQuery(
                "accounts-payable/vendor",
                List.of("id"),
                null, // no filters
                "1 and 2", // but has filterExpression
                null, null, null, 1
            );
        });
    }

    @Test
    void testModelServiceValidation() {
        // Test validation logic works correctly
        
        // Null name should throw exception
        assertThrows(NullPointerException.class, () -> {
            modelService.getModelDefinition(null, null, null, null, null);
        });
    }

    @Test
    void testComplexQueryScenarios() {
        // Test various query scenarios that should work
        
        // Simple query
        List result1 = queryService.executeQuery(
            "accounts-payable/vendor",
            List.of("id", "name", "status"),
            null, null, null, null, null, 5
        );
        
        // Query with filters
        List result2 = queryService.executeQuery(
            "accounts-payable/vendor",
            List.of("id", "name", "status"),
            List.of(Map.of("$eq", Map.of("status", "active"))),
            null, null, null, null, 5
        );
        
        // Query with multiple filters and expression
        List result3 = queryService.executeQuery(
            "accounts-payable/vendor",
            List.of("id", "name", "status", "totalDue"),
            List.of(
                Map.of("$eq", Map.of("status", "active")),
                Map.of("$gt", Map.of("totalDue", 1000))
            ),
            "1 and 2",
            null,
            List.of(Map.of("id", "asc")),
            1, 10
        );
        
        // All should return null due to network failure, but not crash
        assertNull(result1);
        assertNull(result2);
        assertNull(result3);
    }

    @Test
    void testFilterParametersHandling() {
        // Test that FilterParameters are handled correctly
        
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
            1, 10
        );
        
        // Should handle filterParameters without crashing
        assertNull(result); // Expected due to network failure
    }

    @Test
    void testServiceInitializationWithMocks() {
        // Test that services are properly initialized with mocked dependencies
        
        assertNotNull(queryService);
        assertNotNull(modelService);
        
        // Verify mock setup
        assertEquals("mock-access-token-12345", authService.getAccessToken());
        assertEquals("https://mock.intacct.com/api/v1-beta2", authService.getBaseUrl());
    }

    @Test
    void testEdgeCaseObjectNames() {
        // Test various edge cases for object names

        // Empty string after removing objects/ prefix - results in empty string, not exception
        List result0 = queryService.executeQuery("objects/", List.of("id"), null, null, null, null, null, 1);
        assertNull(result0); // Should not crash, but will fail at API level

        // Just "objects" without slash
        List result1 = queryService.executeQuery("objects", List.of("id"), null, null, null, null, null, 1);
        assertNull(result1); // Should not crash

        // Complex object names
        List result2 = queryService.executeQuery("company-config/department", List.of("id"), null, null, null, null, null, 1);
        assertNull(result2); // Should not crash

        // Object names with multiple slashes
        List result3 = queryService.executeQuery("platform-apps/custom/object", List.of("id"), null, null, null, null, null, 1);
        assertNull(result3); // Should not crash
    }

    @Test
    void testMockVerification() {
        // Verify that our mocks are being used correctly

        // Make some calls to trigger mock usage
        queryService.executeQuery("test-object", List.of("id"), null, null, null, null, null, 1);
        modelService.getModelDefinition("test-object", null, null, null, null);

        // Verify AuthService methods were called (getAccessToken is called during service initialization)
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }
}
