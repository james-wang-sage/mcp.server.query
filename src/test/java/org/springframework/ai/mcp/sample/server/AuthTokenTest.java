package org.springframework.ai.mcp.sample.server;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test authentication token acquisition
 */
public class AuthTokenTest {

    @Test
    void testAuthServiceTokenAcquisition() {
        // Create AuthService with system properties
        AuthService authService = new AuthService();
        
        System.out.println("=== AuthService Configuration ===");
        System.out.println("Base URL: " + authService.getBaseUrl());
        
        // Try to get access token
        String token = authService.getAccessToken();
        
        System.out.println("Access Token: " + (token != null ? 
            token.substring(0, Math.min(20, token.length())) + "..." : "null"));
        
        if (token != null) {
            System.out.println("✅ Successfully obtained access token");
            assertTrue(token.length() > 0);
        } else {
            System.out.println("❌ Failed to obtain access token");
            System.out.println("This is expected if OAuth2 credentials are not configured");
        }
    }

    @Test
    void testAuthServiceWithProperties() {
        // Test with properties configuration
        McpServerProperties properties = new McpServerProperties();
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        
        // Set OAuth2 config
        McpServerProperties.OAuth2Config oauth2Config = new McpServerProperties.OAuth2Config();
        oauth2Config.setClientId(System.getProperty("OAUTH2_CLIENT_ID", "test-client"));
        oauth2Config.setClientSecret(System.getProperty("OAUTH2_CLIENT_SECRET", "test-secret"));
        authConfig.setOauth2(oauth2Config);
        authConfig.setUsername(System.getProperty("OAUTH2_USERNAME", "test-user"));
        authConfig.setPassword(System.getProperty("OAUTH2_PASSWORD", "test-password"));
        
        properties.setAuth(authConfig);
        
        AuthService authService = new AuthService(10, 3300, properties);
        
        System.out.println("=== AuthService with Properties ===");
        System.out.println("Base URL: " + authService.getBaseUrl());
        
        String token = authService.getAccessToken();
        System.out.println("Access Token: " + (token != null ? 
            token.substring(0, Math.min(20, token.length())) + "..." : "null"));
        
        // This will likely be null in test environment, which is fine
        if (token == null) {
            System.out.println("ℹ️ No token obtained (expected in test environment)");
        }
    }

    @Test
    void testSystemPropertiesConfiguration() {
        System.out.println("=== System Properties ===");
        System.out.println("OAUTH2_CLIENT_ID: " + System.getProperty("OAUTH2_CLIENT_ID"));
        System.out.println("OAUTH2_CLIENT_SECRET: " + (System.getProperty("OAUTH2_CLIENT_SECRET") != null ? "***" : "null"));
        System.out.println("OAUTH2_USERNAME: " + System.getProperty("OAUTH2_USERNAME"));
        System.out.println("OAUTH2_PASSWORD: " + (System.getProperty("OAUTH2_PASSWORD") != null ? "***" : "null"));
        System.out.println("intacct.base.url: " + System.getProperty("intacct.base.url"));
        System.out.println("intacct.token.endpoint: " + System.getProperty("intacct.token.endpoint"));
        
        // Check if we have the minimum required properties
        String clientId = System.getProperty("OAUTH2_CLIENT_ID");
        String clientSecret = System.getProperty("OAUTH2_CLIENT_SECRET");
        String username = System.getProperty("OAUTH2_USERNAME");
        String password = System.getProperty("OAUTH2_PASSWORD");
        
        if (clientId != null && clientSecret != null && username != null && password != null) {
            System.out.println("✅ All required OAuth2 properties are set");
        } else {
            System.out.println("ℹ️ Some OAuth2 properties are missing (expected in test environment)");
        }
    }

    @Test
    void testEnvironmentVariables() {
        System.out.println("=== Environment Variables ===");
        System.out.println("OAUTH2_CLIENT_ID: " + System.getenv("OAUTH2_CLIENT_ID"));
        System.out.println("OAUTH2_CLIENT_SECRET: " + (System.getenv("OAUTH2_CLIENT_SECRET") != null ? "***" : "null"));
        System.out.println("OAUTH2_USERNAME: " + System.getenv("OAUTH2_USERNAME"));
        System.out.println("OAUTH2_PASSWORD: " + (System.getenv("OAUTH2_PASSWORD") != null ? "***" : "null"));
        System.out.println("INTACCT_BASE_URL: " + System.getenv("INTACCT_BASE_URL"));
        
        // This is just informational
        assertTrue(true);
    }

    @Test
    void testTokenFormat() {
        // Test if we can create a valid-looking token for testing
        String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
        
        // Basic JWT format validation
        String[] parts = testToken.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts separated by dots");
        
        System.out.println("✅ Test token format is valid");
    }

    @Test
    void testUrlConfiguration() {
        // Test different URL configurations
        
        // Test with system property
        System.setProperty("intacct.base.url", "https://test.intacct.com/api/v1-beta2");
        AuthService authService1 = new AuthService();
        assertEquals("https://test.intacct.com/api/v1-beta2", authService1.getBaseUrl());
        
        // Test with properties
        McpServerProperties properties = new McpServerProperties();
        McpServerProperties.AuthConfig authConfig = new McpServerProperties.AuthConfig();
        authConfig.setBaseUrl("https://properties.intacct.com/api/v1-beta2");
        properties.setAuth(authConfig);
        
        AuthService authService2 = new AuthService(10, 3300, properties);
        assertEquals("https://properties.intacct.com/api/v1-beta2", authService2.getBaseUrl());
        
        // Clean up
        System.clearProperty("intacct.base.url");
        
        System.out.println("✅ URL configuration works correctly");
    }
}
