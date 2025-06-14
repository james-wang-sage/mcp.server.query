package com.intacct.ds.mcp.server.query.transport;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.intacct.ds.mcp.server.query.config.McpServerProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Manages the initialization and lifecycle of STDIO transport.
 */
@Component
public class TransportManager {
    private static final Logger logger = LoggerFactory.getLogger(TransportManager.class);

    private final McpServerProperties properties;
    private final ApplicationContext applicationContext;
    private StdioTransport stdioTransport;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public TransportManager(McpServerProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing MCP transport manager...");
        initializeStdioTransport();
        initialized.set(true);
    }

    private void initializeStdioTransport() {
        logger.info("Initializing STDIO transport...");
        stdioTransport = new StdioTransport(properties, applicationContext);
        stdioTransport.initialize();
        logger.info("STDIO transport initialized successfully");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MCP transport manager...");
        if (stdioTransport != null) {
            stdioTransport.shutdown();
            logger.info("STDIO transport shut down successfully");
        }
        initialized.set(false);
    }

    /**
     * Check if the transport manager is initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Check if STDIO transport is active
     */
    public boolean isTransportActive(TransportMode mode) {
        return initialized.get() && stdioTransport != null;
    }
}
