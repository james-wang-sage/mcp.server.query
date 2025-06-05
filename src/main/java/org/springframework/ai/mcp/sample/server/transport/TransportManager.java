package org.springframework.ai.mcp.sample.server.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.ai.mcp.sample.server.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Manages the initialization and lifecycle of different MCP transport modes.
 * Coordinates between STDIO and SSE transports based on configuration.
 */
@Component
public class TransportManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TransportManager.class);
    
    private final McpServerProperties properties;
    private final SecurityContext securityContext;
    private final List<TransportMode> activeTransports = new ArrayList<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    @Autowired
    public TransportManager(McpServerProperties properties, SecurityContext securityContext) {
        this.properties = properties;
        this.securityContext = securityContext;
    }
    
    /**
     * Initialize transports after the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeTransports() {
        if (initialized.compareAndSet(false, true)) {
            logger.info("Initializing MCP transports...");
            
            // Initialize STDIO transport if enabled
            if (properties.getStdio().isEnabled()) {
                initializeStdioTransport();
            }
            
            // Initialize SSE transport if enabled
            if (properties.getSse().isEnabled()) {
                initializeSseTransport();
            }
            
            if (activeTransports.isEmpty()) {
                logger.warn("No transports are enabled! Server may not be accessible.");
            } else {
                logger.info("Initialized transports: {}", activeTransports);
            }
        }
    }
    
    /**
     * Initialize STDIO transport
     */
    private void initializeStdioTransport() {
        try {
            logger.info("Initializing STDIO transport...");
            
            // Set up STDIO-specific configuration
            if (properties.getStdio().isDisableConsoleLogging()) {
                logger.debug("Console logging disabled for STDIO compatibility");
            }
            
            if (properties.getStdio().isDisableBanner()) {
                logger.debug("Spring Boot banner disabled for STDIO compatibility");
            }
            
            // Set default security context for STDIO mode
            securityContext.setAuthenticationContext(SecurityContext.createStdioContext());
            
            activeTransports.add(TransportMode.STDIO);
            logger.info("STDIO transport initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize STDIO transport", e);
            throw new TransportInitializationException("STDIO transport initialization failed", e);
        }
    }
    
    /**
     * Initialize SSE transport
     */
    private void initializeSseTransport() {
        try {
            logger.info("Initializing SSE transport on port {}...", properties.getSse().getPort());
            
            // Validate SSE configuration
            validateSseConfiguration();
            
            // SSE transport is handled by Spring Boot's embedded server
            // The actual endpoints will be registered by the SSE controller
            
            activeTransports.add(TransportMode.SSE);
            logger.info("SSE transport initialized successfully on port {}", properties.getSse().getPort());
            
        } catch (Exception e) {
            logger.error("Failed to initialize SSE transport", e);
            throw new TransportInitializationException("SSE transport initialization failed", e);
        }
    }
    
    /**
     * Validate SSE configuration
     */
    private void validateSseConfiguration() {
        McpServerProperties.SseConfig sseConfig = properties.getSse();
        
        if (sseConfig.getPort() <= 0 || sseConfig.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid SSE port: " + sseConfig.getPort());
        }
        
        if (sseConfig.getPath() == null || sseConfig.getPath().trim().isEmpty()) {
            throw new IllegalArgumentException("SSE path cannot be empty");
        }
        
        if (!sseConfig.getPath().startsWith("/")) {
            throw new IllegalArgumentException("SSE path must start with '/': " + sseConfig.getPath());
        }
        
        if (sseConfig.getMaxConnections() <= 0) {
            throw new IllegalArgumentException("Max connections must be positive: " + sseConfig.getMaxConnections());
        }
        
        if (sseConfig.getConnectionTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive: " + sseConfig.getConnectionTimeoutSeconds());
        }
        
        // Validate authentication configuration for SSE
        if (properties.getAuth().getMode() == McpServerProperties.AuthMode.OAUTH2) {
            validateOAuth2Configuration();
        }
        
        logger.debug("SSE configuration validation passed");
    }
    
    /**
     * Validate OAuth2 configuration
     */
    private void validateOAuth2Configuration() {
        McpServerProperties.OAuth2Config oauth2Config = properties.getAuth().getOauth2();
        
        if (oauth2Config.getClientId() == null || oauth2Config.getClientId().trim().isEmpty()) {
            throw new IllegalArgumentException("OAuth2 client ID is required for SSE mode");
        }
        
        if (oauth2Config.getClientSecret() == null || oauth2Config.getClientSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("OAuth2 client secret is required for SSE mode");
        }
        
        if (oauth2Config.getAuthorizationUri() == null || oauth2Config.getAuthorizationUri().trim().isEmpty()) {
            throw new IllegalArgumentException("OAuth2 authorization URI is required for SSE mode");
        }
        
        if (oauth2Config.getTokenUri() == null || oauth2Config.getTokenUri().trim().isEmpty()) {
            throw new IllegalArgumentException("OAuth2 token URI is required for SSE mode");
        }
        
        if (oauth2Config.getRedirectUri() == null || oauth2Config.getRedirectUri().trim().isEmpty()) {
            throw new IllegalArgumentException("OAuth2 redirect URI is required for SSE mode");
        }
        
        logger.debug("OAuth2 configuration validation passed");
    }
    
    /**
     * Check if a specific transport mode is active
     */
    public boolean isTransportActive(TransportMode mode) {
        return activeTransports.contains(mode);
    }
    
    /**
     * Get all active transport modes
     */
    public List<TransportMode> getActiveTransports() {
        return new ArrayList<>(activeTransports);
    }
    
    /**
     * Check if the transport manager is initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * Get the primary transport mode (first active transport)
     */
    public TransportMode getPrimaryTransport() {
        return activeTransports.isEmpty() ? null : activeTransports.get(0);
    }
    
    /**
     * Check if dual mode is active (both STDIO and SSE)
     */
    public boolean isDualModeActive() {
        return activeTransports.contains(TransportMode.STDIO) && 
               activeTransports.contains(TransportMode.SSE);
    }
    
    /**
     * Shutdown all transports gracefully
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down transport manager...");
        
        // Clear security context
        securityContext.clear();
        
        // Clear active transports
        activeTransports.clear();
        
        // Reset initialization flag
        initialized.set(false);
        
        logger.info("Transport manager shutdown complete");
    }
    
    /**
     * Exception thrown when transport initialization fails
     */
    public static class TransportInitializationException extends RuntimeException {
        public TransportInitializationException(String message) {
            super(message);
        }
        
        public TransportInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}