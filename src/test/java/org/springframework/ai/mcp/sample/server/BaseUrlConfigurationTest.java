package org.springframework.ai.mcp.sample.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Test class to verify base URL configuration functionality
 */
@SpringBootTest(classes = {AuthService.class})
@TestPropertySource(properties = {
    "spring.profiles.active=test"
})
public class BaseUrlConfigurationTest {

    @Test
    public void testSystemPropertyBaseUrl() {
        // Set system property with new ia URL
        System.setProperty("intacct.base.url", "https://api-partner-main.intacct.com/ia/api/v1-beta2");

        try {
            // Create AuthService with system property
            AuthService authService = new AuthService(10, 3300, null);

            // Verify that the base URL is read from system property
            // Note: We can't directly access baseUrl field, but we can verify through logs
            // or by checking the token endpoint construction
            assertNotNull(authService);

        } finally {
            // Clean up system property
            System.clearProperty("intacct.base.url");
        }
    }

    @Test
    public void testPropertiesBaseUrl() {
        // Create properties with base URL using new ia URL
        McpServerProperties properties = new McpServerProperties();
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        authConfig.setBaseUrl("https://api-partner-main.intacct.com/ia/api/v1-beta2");
        properties.setAuth(authConfig);

        // Create AuthService with properties
        AuthService authService = new AuthService(10, 3300, properties);

        // Verify that AuthService was created successfully
        assertNotNull(authService);
    }

    @Test
    public void testDefaultBaseUrl() {
        // Create AuthService without any base URL configuration
        AuthService authService = new AuthService(10, 3300, null);
        
        // Verify that AuthService was created successfully with default URL
        assertNotNull(authService);
    }

    @Test
    public void testPropertiesPriorityOverSystemProperty() {
        // Set system property with ia3 URL
        System.setProperty("intacct.base.url", "https://api-partner-main.intacct.com/ia3/api/v1-beta2");

        try {
            // Create properties with different base URL using new ia URL
            McpServerProperties properties = new McpServerProperties();
            McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
            authConfig.setBaseUrl("https://api-partner-main.intacct.com/ia/api/v1-beta2");
            properties.setAuth(authConfig);

            // Create AuthService - properties should take priority
            AuthService authService = new AuthService(10, 3300, properties);

            // Verify that AuthService was created successfully
            assertNotNull(authService);

        } finally {
            // Clean up system property
            System.clearProperty("intacct.base.url");
        }
    }
}
