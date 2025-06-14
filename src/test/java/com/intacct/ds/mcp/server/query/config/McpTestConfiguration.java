package com.intacct.ds.mcp.server.query.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.intacct.ds.mcp.server.query.service.AuthService;

/**
 * Test configuration that provides a test-specific AuthService for testing.
 */
@TestConfiguration
public class McpTestConfiguration {

    /**
     * Provides a test AuthService that returns a dummy token without making HTTP calls.
     * This bean will override the real AuthService in test contexts.
     */
    @Bean
    @Primary
    public AuthService testAuthService() {
        return new AuthService() {
            @Override
            public String getAccessToken() {
                return "test-access-token";
            }
            
            @Override
            public void init() {
                // Do nothing in tests - skip initialization
            }
        };
    }
}
