package org.springframework.ai.mcp.sample.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable MCP server configuration properties.
 */
@Configuration
@EnableConfigurationProperties(McpServerProperties.class)
public class McpServerConfiguration {
    // This class enables the configuration properties binding
}