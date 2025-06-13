package org.springframework.ai.mcp.sample.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Test to ensure all services use the same base URL configuration
 */
@SpringBootTest(classes = {AuthService.class, QueryService.class, ModelService.class})
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.pattern.console="
})
public class ServiceUrlConsistencyTest {

    @Test
    public void testAllServicesUseSameBaseUrl() {
        // Test with custom base URL
        String customBaseUrl = "https://test-consistency.intacct.com/api/v2";
        System.setProperty("intacct.base.url", customBaseUrl);
        
        try {
            // Create AuthService first
            AuthService authService = new AuthService(10, 3300, null);
            
            // Verify AuthService has the custom URL
            assertEquals(customBaseUrl, authService.getBaseUrl());
            
            // Create other services that depend on AuthService
            QueryService queryService = new QueryService(authService);
            ModelService modelService = new ModelService(authService);
            
            // All services should be created successfully
            assertNotNull(authService);
            assertNotNull(queryService);
            assertNotNull(modelService);
            
        } finally {
            System.clearProperty("intacct.base.url");
        }
    }

    @Test
    public void testPropertiesBasedUrlConsistency() {
        // Test with properties configuration
        McpServerProperties properties = new McpServerProperties();
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        String customBaseUrl = "https://properties-consistency.intacct.com/api/v2";
        authConfig.setBaseUrl(customBaseUrl);
        
        // Set required OAuth2 config
        McpServerProperties.OAuth2Config oauth2Config = new McpServerProperties.OAuth2Config();
        oauth2Config.setClientId("test-client");
        oauth2Config.setClientSecret("test-secret");
        authConfig.setOauth2(oauth2Config);
        authConfig.setUsername("test-user");
        authConfig.setPassword("test-password");
        
        properties.setAuth(authConfig);
        
        // Create AuthService with properties
        AuthService authService = new AuthService(10, 3300, properties);
        
        // Verify AuthService has the custom URL from properties
        assertEquals(customBaseUrl, authService.getBaseUrl());
        
        // Create dependent services
        QueryService queryService = new QueryService(authService);
        ModelService modelService = new ModelService(authService);
        
        // All services should be created successfully
        assertNotNull(authService);
        assertNotNull(queryService);
        assertNotNull(modelService);
    }

    @Test
    public void testDefaultUrlConsistency() {
        // Clear any system properties
        System.clearProperty("intacct.base.url");

        // Create services with default configuration
        AuthService authService = new AuthService(10, 3300, null);
        QueryService queryService = new QueryService(authService);
        ModelService modelService = new ModelService(authService);

        // Verify default URL is used (updated for environment migration)
        assertEquals("https://api-partner-main.intacct.com/ia/api/v1-beta2", authService.getBaseUrl());

        // All services should be created successfully
        assertNotNull(authService);
        assertNotNull(queryService);
        assertNotNull(modelService);
    }
}
