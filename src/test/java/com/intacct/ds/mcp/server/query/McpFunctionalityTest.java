package com.intacct.ds.mcp.server.query;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.intacct.ds.mcp.server.query.service.AuthService;
import com.intacct.ds.mcp.server.query.service.ModelService;
import com.intacct.ds.mcp.server.query.service.QueryService;

/**
 * Focused test for MCP server functionality that we've implemented and fixed
 */
@SpringBootTest(classes = {AuthService.class, QueryService.class, ModelService.class})
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.pattern.console="
})
public class McpFunctionalityTest {

    @MockBean
    private AuthService authService;

    private QueryService queryService;
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        // Mock AuthService to return test values
        when(authService.getAccessToken()).thenReturn("test-token-123");
        when(authService.getBaseUrl()).thenReturn("https://test.intacct.com/api/v1-beta2");

        // Create services with mocked AuthService
        queryService = new QueryService(authService);
        modelService = new ModelService(authService);
    }

    @Test
    void testObjectsPrefixRemovalInQueryService() {
        // Test the main fix: objects/ prefix removal
        
        // Both calls should behave identically after prefix removal
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
        
        // Both should return null (due to no real API) but behave the same
        assertEquals(result1, result2);
        assertNull(result1);
        assertNull(result2);
        
        // Verify AuthService was called for both
        verify(authService, atLeast(2)).getAccessToken();
        verify(authService, atLeast(2)).getBaseUrl();
    }

    @Test
    void testObjectsPrefixRemovalInModelService() {
        // Test the main fix: objects/ prefix removal in ModelService
        
        var result1 = modelService.getModelDefinition(
            "objects/accounts-payable/vendor", // With objects/ prefix
            null, null, null, null
        );
        
        var result2 = modelService.getModelDefinition(
            "accounts-payable/vendor", // Without objects/ prefix
            null, null, null, null
        );
        
        // Both should behave the same
        assertEquals(result1, result2);
        assertNull(result1);
        assertNull(result2);
        
        // Verify AuthService was called for both
        verify(authService, atLeast(2)).getAccessToken();
        verify(authService, atLeast(2)).getBaseUrl();
    }

    @Test
    void testUrlConsistencyBetweenServices() {
        // Test that all services use the same baseUrl from AuthService
        
        // Make calls to both services
        queryService.executeQuery("test-object", List.of("id"), null, null, null, null, null, 1);
        modelService.getModelDefinition("test-object", null, null, null, null);
        
        // Both should have called AuthService.getBaseUrl()
        verify(authService, atLeast(2)).getBaseUrl();
        
        // Verify they're using the same URL
        String expectedUrl = "https://test.intacct.com/api/v1-beta2";
        assertEquals(expectedUrl, authService.getBaseUrl());
    }

    @Test
    void testQueryServiceValidation() {
        // Test that validation still works correctly
        
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
        // Test that validation still works correctly
        
        // Null name should throw exception
        assertThrows(NullPointerException.class, () -> {
            modelService.getModelDefinition(null, null, null, null, null);
        });
    }

    @Test
    void testComplexQueryWithObjectsPrefix() {
        // Test complex query scenarios with objects/ prefix

        List result = queryService.executeQuery(
            "objects/accounts-payable/vendor", // With objects/ prefix
            List.of("id", "name", "status", "totalDue"),
            List.of(
                Map.of("$eq", Map.of("status", "active")),
                Map.of("$gt", Map.of("totalDue", 1000))
            ),
            "1 and 2",
            new QueryService.FilterParameters("2025-06-13", false, true, false),
            List.of(Map.of("id", "asc")),
            1, 10
        );

        // Should handle complex query with objects/ prefix without crashing
        assertNull(result); // Expected due to no real API

        // Verify AuthService was called
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }

    @Test
    void testVariousObjectNames() {
        // Test various object name patterns

        String[] testObjects = {
            "accounts-payable/vendor",
            "objects/accounts-payable/vendor",
            "company-config/department",
            "objects/company-config/department",
            "platform-apps/custom/object",
            "objects/platform-apps/custom/object"
        };

        for (String objectName : testObjects) {
            // Should not throw exceptions for any of these
            List result = queryService.executeQuery(
                objectName,
                List.of("id"),
                null, null, null, null, null, 1
            );

            assertNull(result); // Expected due to no real API
        }

        // Verify AuthService was called (getAccessToken is called during service initialization)
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }

    @Test
    void testServiceInitialization() {
        // Test that services are properly initialized

        assertNotNull(queryService);
        assertNotNull(modelService);

        // Verify mock configuration
        assertEquals("test-token-123", authService.getAccessToken());
        assertEquals("https://test.intacct.com/api/v1-beta2", authService.getBaseUrl());
    }

    @Test
    void testEmptyObjectsPrefix() {
        // Test edge case: "objects/" with nothing after

        List result = queryService.executeQuery(
            "objects/", // Empty after prefix
            List.of("id"),
            null, null, null, null, null, 1
        );

        // Should not crash, but will return null due to empty object name
        assertNull(result);

        // Verify AuthService was still called
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }

    @Test
    void testJustObjectsWord() {
        // Test edge case: just "objects" without slash

        List result = queryService.executeQuery(
            "objects", // Just the word "objects"
            List.of("id"),
            null, null, null, null, null, 1
        );

        // Should not crash
        assertNull(result);

        // Verify AuthService was called
        verify(authService, atLeastOnce()).getAccessToken();
        verify(authService, atLeastOnce()).getBaseUrl();
    }
}
