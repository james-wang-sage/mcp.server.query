package org.springframework.ai.mcp.sample.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.transport.TransportMode;
import org.springframework.stereotype.Component;

/**
 * Manages the security context for the MCP server.
 * Currently only supports STDIO transport mode.
 */
@Component
public class SecurityContext {
    private static final Logger logger = LoggerFactory.getLogger(SecurityContext.class);

    private AuthenticationContext context;

    /**
     * Set the current authentication context
     */
    public void setAuthenticationContext(AuthenticationContext context) {
        this.context = context;
    }

    /**
     * Clear the current authentication context
     */
    public void clear() {
        this.context = null;
    }

    /**
     * Get the current authentication context
     */
    public AuthenticationContext getAuthenticationContext() {
        return context;
    }

    /**
     * Check if the current context is authenticated
     */
    public boolean isAuthenticated() {
        if (context == null) {
            return false;
        }

        if (context.getAccessToken() == null) {
            logger.warn("Context is not authenticated");
            return false;
        }

        if (context.isExpired()) {
            logger.warn("Context token is expired");
            return false;
        }

        return true;
    }

    /**
     * Get the current access token
     */
    public String getAccessToken() {
        if (!isAuthenticated()) {
            return null;
        }
        return context.getAccessToken();
    }

    /**
     * Create a STDIO authentication context
     */
    public static AuthenticationContext createStdioContext() {
        return new AuthenticationContext(TransportMode.STDIO);
    }
}