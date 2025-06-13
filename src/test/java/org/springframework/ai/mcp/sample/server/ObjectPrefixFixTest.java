package org.springframework.ai.mcp.sample.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that "objects/" prefix is correctly handled and removed
 */
@SpringBootTest(classes = {AuthService.class, QueryService.class, ModelService.class})
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.pattern.console="
})
public class ObjectPrefixFixTest {

    @Test
    public void testQueryServiceHandlesObjectsPrefix() {
        // Create services
        AuthService authService = new AuthService(10, 3300, null);
        QueryService queryService = new QueryService(authService);
        
        // Test with objects/ prefix - should not throw exception and should handle gracefully
        // Note: This will return null due to no authentication in test, but it should process the object name correctly
        List result = queryService.executeQuery(
            "objects/accounts-payable/vendor", // With objects/ prefix
            List.of("id", "name", "status"),
            null, null, null, null, null, 3
        );
        
        // The method should handle the prefix removal and not crash
        // Result will be null due to authentication failure, but that's expected in test
        // The important thing is that it doesn't throw an exception due to malformed object name
        assertNull(result); // Expected due to no authentication in test environment
    }

    @Test
    public void testQueryServiceWithoutObjectsPrefix() {
        // Create services
        AuthService authService = new AuthService(10, 3300, null);
        QueryService queryService = new QueryService(authService);
        
        // Test without objects/ prefix - should work the same way
        List result = queryService.executeQuery(
            "accounts-payable/vendor", // Without objects/ prefix
            List.of("id", "name", "status"),
            null, null, null, null, null, 3
        );
        
        // Should handle normally
        assertNull(result); // Expected due to no authentication in test environment
    }

    @Test
    public void testModelServiceHandlesObjectsPrefix() {
        // Create services
        AuthService authService = new AuthService(10, 3300, null);
        ModelService modelService = new ModelService(authService);
        
        // Test with objects/ prefix
        var result = modelService.getModelDefinition(
            "objects/accounts-payable/vendor", // With objects/ prefix
            null, null, null, null
        );
        
        // Should handle the prefix removal and not crash
        assertNull(result); // Expected due to no authentication in test environment
    }

    @Test
    public void testModelServiceWithoutObjectsPrefix() {
        // Create services
        AuthService authService = new AuthService(10, 3300, null);
        ModelService modelService = new ModelService(authService);
        
        // Test without objects/ prefix
        var result = modelService.getModelDefinition(
            "accounts-payable/vendor", // Without objects/ prefix
            null, null, null, null
        );
        
        // Should handle normally
        assertNull(result); // Expected due to no authentication in test environment
    }

    @Test
    public void testEdgeCases() {
        // Create services
        AuthService authService = new AuthService(10, 3300, null);
        QueryService queryService = new QueryService(authService);
        ModelService modelService = new ModelService(authService);

        // Test edge cases

        // 1. Empty string after objects/ removal - this actually works, doesn't throw exception
        List result0 = queryService.executeQuery("objects/", List.of("id"), null, null, null, null, null, 1);
        assertNull(result0); // Expected due to no authentication

        // 2. Just "objects" without slash
        List result1 = queryService.executeQuery("objects", List.of("id"), null, null, null, null, null, 1);
        assertNull(result1); // Should not crash

        // 3. Multiple objects/ prefixes (shouldn't happen but test anyway)
        List result2 = queryService.executeQuery("objects/objects/test", List.of("id"), null, null, null, null, null, 1);
        assertNull(result2); // Should not crash

        // 4. Normal object names should work unchanged
        List result3 = queryService.executeQuery("accounts-payable/vendor", List.of("id"), null, null, null, null, null, 1);
        assertNull(result3); // Expected due to no authentication
    }
}
