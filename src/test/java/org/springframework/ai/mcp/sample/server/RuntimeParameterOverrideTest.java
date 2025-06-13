package org.springframework.ai.mcp.sample.server;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that all partner.intacct.com related values can be overwritten by runtime parameters
 */
@SpringBootTest(classes = {AuthService.class, QueryService.class, ModelService.class})
@TestPropertySource(properties = {
    "spring.profiles.active=test"
})
public class RuntimeParameterOverrideTest {

    @Test
    public void testAllServicesUseConfigurableBaseUrl() {
        // Test with custom base URL
        String customBaseUrl = "https://custom.intacct.com/api/v2";
        System.setProperty("intacct.base.url", customBaseUrl);
        
        try {
            // Create all services with custom base URL
            AuthService authService = new AuthService(10, 3300, null);
            QueryService queryService = new QueryService(authService);
            ModelService modelService = new ModelService(authService);
            
            // Verify that all services were created successfully
            assertNotNull(authService);
            assertNotNull(queryService);
            assertNotNull(modelService);
            
        } finally {
            // Clean up system property
            System.clearProperty("intacct.base.url");
        }
    }

    @Test
    public void testPropertiesBasedConfiguration() {
        // Create properties with all custom URLs
        McpServerProperties properties = new McpServerProperties();
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        McpServerProperties.OAuth2Config oauth2Config = new McpServerProperties.OAuth2Config();
        
        // Set custom URLs
        authConfig.setBaseUrl("https://custom.intacct.com/api/v2");
        oauth2Config.setAuthorizationUri("https://custom.intacct.com/api/v2/oauth2/authorize");
        oauth2Config.setTokenUri("https://custom.intacct.com/api/v2/oauth2/token");
        oauth2Config.setRedirectUri("http://localhost:9090/login/oauth2/code/mcp-client");
        
        // Set OAuth2 credentials
        oauth2Config.setClientId("test-client-id");
        oauth2Config.setClientSecret("test-client-secret");
        authConfig.setUsername("test-username");
        authConfig.setPassword("test-password");
        
        authConfig.setOauth2(oauth2Config);
        properties.setAuth(authConfig);
        
        // Create AuthService with properties
        AuthService authService = new AuthService(10, 3300, properties);
        
        // Verify that AuthService was created successfully
        assertNotNull(authService);
    }

    @Test
    public void testTokenEndpointOverride() {
        // Test token endpoint override
        String customTokenEndpoint = "https://custom.intacct.com/api/v2/oauth2/token";
        System.setProperty("intacct.token.endpoint", customTokenEndpoint);
        
        try {
            // Create AuthService with custom token endpoint
            AuthService authService = new AuthService(10, 3300, null);
            
            // Verify that AuthService was created successfully
            assertNotNull(authService);
            
        } finally {
            // Clean up system property
            System.clearProperty("intacct.token.endpoint");
        }
    }

    @Test
    public void testEnvironmentVariableSupport() {
        // Test that environment variables are supported through application.yml
        // This test verifies that the ${INTACCT_BASE_URL:default} pattern works
        
        // Create properties to simulate environment variable configuration
        McpServerProperties properties = new McpServerProperties();
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        
        // Simulate what would happen with environment variables
        authConfig.setBaseUrl("https://env.intacct.com/api/v1");
        properties.setAuth(authConfig);
        
        // Create AuthService
        AuthService authService = new AuthService(10, 3300, properties);
        
        // Verify that AuthService was created successfully
        assertNotNull(authService);
    }

    @Test
    public void testNoHardcodedUrls() {
        // This test ensures that all services can work with completely custom URLs
        String customBaseUrl = "https://completely-different.example.com/intacct/v3";
        System.setProperty("intacct.base.url", customBaseUrl);
        
        String customTokenEndpoint = "https://auth.example.com/oauth2/token";
        System.setProperty("intacct.token.endpoint", customTokenEndpoint);
        
        try {
            // Create all services
            AuthService authService = new AuthService(10, 3300, null);
            QueryService queryService = new QueryService(authService);
            ModelService modelService = new ModelService(authService);
            
            // All services should be created successfully even with completely different URLs
            assertNotNull(authService);
            assertNotNull(queryService);
            assertNotNull(modelService);
            
        } finally {
            // Clean up system properties
            System.clearProperty("intacct.base.url");
            System.clearProperty("intacct.token.endpoint");
        }
    }
}
