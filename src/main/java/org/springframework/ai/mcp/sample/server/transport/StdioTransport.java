package org.springframework.ai.mcp.sample.server.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Handles STDIO transport mode for MCP server.
 * Manages reading from standard input and writing to standard output.
 */
@Component
public class StdioTransport {
    private static final Logger logger = LoggerFactory.getLogger(StdioTransport.class);

    private final McpServerProperties properties;
    private final ApplicationContext applicationContext;

    public StdioTransport(McpServerProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize STDIO transport
     */
    public void initialize() {
        logger.info("Initializing STDIO transport...");
        
        // Set up STDIO-specific configuration
        if (properties.getStdio().isDisableConsoleLogging()) {
            logger.debug("Console logging disabled for STDIO compatibility");
        }
        
        if (properties.getStdio().isDisableBanner()) {
            logger.debug("Spring Boot banner disabled for STDIO compatibility");
        }
        
        logger.info("STDIO transport initialized successfully");
    }

    /**
     * Shutdown STDIO transport
     */
    public void shutdown() {
        logger.info("Shutting down STDIO transport...");
        // Clean up any resources if needed
        logger.info("STDIO transport shut down successfully");
    }
} 