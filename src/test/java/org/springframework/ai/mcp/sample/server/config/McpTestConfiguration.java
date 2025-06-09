package org.springframework.ai.mcp.sample.server.config;

import org.mockito.Mockito;
import org.springframework.ai.mcp.sample.server.AuthService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides mocked beans for testing.
 */
@TestConfiguration
public class McpTestConfiguration {

    /**
     * Provides a mocked AuthService that returns a dummy token without making HTTP calls.
     * This bean will override the real AuthService in test contexts.
     */
    @Bean
    @Primary
    public AuthService mockAuthService() {
        AuthService mockAuthService = Mockito.mock(AuthService.class);
        // Return a dummy token for tests
        Mockito.when(mockAuthService.getAccessToken()).thenReturn("test-access-token");
        return mockAuthService;
    }
}