package org.springframework.ai.mcp.sample.server.security;

import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 security configuration for MCP server.
 * Currently only supports STDIO transport mode.
 */
@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig {

    private final McpServerProperties properties;

    public OAuth2SecurityConfig(McpServerProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable security for STDIO mode
        http.csrf().disable()
            .authorizeHttpRequests()
            .anyRequest().permitAll();
        
        return http.build();
    }
} 