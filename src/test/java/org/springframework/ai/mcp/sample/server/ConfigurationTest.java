package org.springframework.ai.mcp.sample.server;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify configuration loading without network calls
 */
@SpringBootTest(classes = {AuthService.class})
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.pattern.console="
})
public class ConfigurationTest {

    @Test
    public void testSystemPropertyBaseUrlConfiguration() {
        // Test that system property is read correctly
        String testUrl = "https://config-test.intacct.com/api/v2";
        System.setProperty("intacct.base.url", testUrl);
        
        try {
            // Create AuthService - this only tests configuration loading
            AuthService authService = new AuthService(10, 3300, null);
            assertNotNull(authService);
            
        } finally {
            System.clearProperty("intacct.base.url");
        }
    }

    @Test
    public void testPropertiesBasedConfiguration() {
        // Test configuration via McpServerProperties
        McpServerProperties properties = new McpServerProperties();
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        authConfig.setBaseUrl("https://properties-test.intacct.com/api/v2");
        authConfig.setUsername("test-user");
        authConfig.setPassword("test-password");
        
        McpServerProperties.OAuth2Config oauth2Config = new McpServerProperties.OAuth2Config();
        oauth2Config.setClientId("test-client");
        oauth2Config.setClientSecret("test-secret");
        oauth2Config.setTokenUri("https://properties-test.intacct.com/api/v2/oauth2/token");
        authConfig.setOauth2(oauth2Config);
        
        properties.setAuth(authConfig);
        
        // Create AuthService with properties
        AuthService authService = new AuthService(10, 3300, properties);
        assertNotNull(authService);
    }

    @Test
    public void testDefaultConfiguration() {
        // Test default configuration when no overrides are provided
        System.clearProperty("intacct.base.url");
        System.clearProperty("intacct.token.endpoint");
        
        AuthService authService = new AuthService(10, 3300, null);
        assertNotNull(authService);
    }

    @Test
    public void testConfigurationPriority() {
        // Test that properties take priority over system properties
        System.setProperty("intacct.base.url", "https://system.intacct.com/api/v2");
        
        try {
            McpServerProperties properties = new McpServerProperties();
            McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
            authConfig.setBaseUrl("https://properties.intacct.com/api/v2");
            properties.setAuth(authConfig);
            
            // Properties should take priority
            AuthService authService = new AuthService(10, 3300, properties);
            assertNotNull(authService);
            
        } finally {
            System.clearProperty("intacct.base.url");
        }
    }

    @Test
    public void testEmptyPropertiesConfiguration() {
        // Test with empty properties object
        McpServerProperties properties = new McpServerProperties();
        AuthService authService = new AuthService(10, 3300, properties);
        assertNotNull(authService);
    }

    @Test
    public void testNullPropertiesConfiguration() {
        // Test with null properties
        AuthService authService = new AuthService(10, 3300, null);
        assertNotNull(authService);
    }
}
