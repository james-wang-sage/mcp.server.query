package org.springframework.ai.mcp.sample.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.transport.TransportMode;
import org.springframework.stereotype.Component;

/**
 * Thread-local security context that manages authentication state for both
 * STDIO and SSE transport modes. This provides a unified way to access
 * authentication information regardless of the transport mechanism.
 */
@Component
public class SecurityContext {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityContext.class);
    
    private final ThreadLocal<AuthenticationContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Set the authentication context for the current thread
     */
    public void setAuthenticationContext(AuthenticationContext context) {
        if (context == null) {
            logger.warn("Setting null authentication context");
        } else {
            logger.debug("Setting authentication context: {}", context);
        }
        contextHolder.set(context);
    }
    
    /**
     * Get the current authentication context
     */
    public AuthenticationContext getAuthenticationContext() {
        return contextHolder.get();
    }
    
    /**
     * Clear the authentication context for the current thread
     */
    public void clear() {
        logger.debug("Clearing authentication context");
        contextHolder.remove();
    }
    
    /**
     * Check if the current context is in STDIO mode
     */
    public boolean isStdioMode() {
        AuthenticationContext context = contextHolder.get();
        return context != null && context.isStdioMode();
    }
    
    /**
     * Check if the current context is in SSE mode
     */
    public boolean isSseMode() {
        AuthenticationContext context = contextHolder.get();
        return context != null && context.isSseMode();
    }
    
    /**
     * Check if the current context is authenticated
     */
    public boolean isAuthenticated() {
        AuthenticationContext context = contextHolder.get();
        return context != null && context.isAuthenticated() && !context.isExpired();
    }
    
    /**
     * Get the current transport mode
     */
    public TransportMode getTransportMode() {
        AuthenticationContext context = contextHolder.get();
        return context != null ? context.getTransportMode() : null;
    }
    
    /**
     * Get the current session ID (SSE mode only)
     */
    public String getCurrentSessionId() {
        AuthenticationContext context = contextHolder.get();
        if (context == null || !context.isSseMode()) {
            return null;
        }
        return context.getSessionId();
    }
    
    /**
     * Get the current user's access token for API calls.
     * This method handles both STDIO (returns null - uses service account) 
     * and SSE (returns user's OAuth2 token) modes.
     */
    public String getCurrentUserAccessToken() {
        AuthenticationContext context = contextHolder.get();
        if (context == null) {
            logger.warn("No authentication context available");
            return null;
        }
        
        if (context.isStdioMode()) {
            // STDIO mode doesn't use user tokens - services will use service account
            return null;
        }
        
        if (context.isSseMode()) {
            if (!context.isAuthenticated()) {
                logger.warn("SSE context is not authenticated");
                return null;
            }
            
            if (context.isExpired()) {
                logger.warn("SSE context token is expired");
                return null;
            }
            
            return context.getAccessToken();
        }
        
        logger.warn("Unknown transport mode: {}", context.getTransportMode());
        return null;
    }
    
    /**
     * Require authentication for the current context.
     * Throws UnauthorizedException if not authenticated.
     */
    public void requireAuthentication() {
        AuthenticationContext context = contextHolder.get();
        if (context == null) {
            throw new UnauthorizedException("No authentication context");
        }
        
        if (context.isStdioMode()) {
            // STDIO mode is always considered "authenticated" for service operations
            return;
        }
        
        if (!context.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        
        if (context.isExpired()) {
            throw new UnauthorizedException("Authentication expired");
        }
    }
    
    /**
     * Create a STDIO authentication context
     */
    public static AuthenticationContext createStdioContext() {
        return new AuthenticationContext(TransportMode.STDIO);
    }
    
    /**
     * Create an SSE authentication context
     */
    public static AuthenticationContext createSseContext(String sessionId, String accessToken, long expiresAt) {
        return new AuthenticationContext(TransportMode.SSE, sessionId, accessToken, expiresAt);
    }
    
    /**
     * Exception thrown when authentication is required but not present or invalid
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
        
        public UnauthorizedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}