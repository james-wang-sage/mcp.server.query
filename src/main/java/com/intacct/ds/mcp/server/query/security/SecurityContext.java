package com.intacct.ds.mcp.server.query.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provides access to the current security context.
 */
@Component
public class SecurityContext {
    private static final Logger logger = LoggerFactory.getLogger(SecurityContext.class);

    private static final ThreadLocal<AuthenticationContext> context = new ThreadLocal<>();

    /**
     * Get the current authentication context.
     * If no context exists, creates a new one for STDIO mode.
     */
    public static AuthenticationContext getContext() {
        AuthenticationContext authContext = context.get();
        if (authContext == null) {
            authContext = new AuthenticationContext();
            context.set(authContext);
        }
        return authContext;
    }

    /**
     * Set the current authentication context.
     */
    public static void setContext(AuthenticationContext authContext) {
        context.set(authContext);
    }

    /**
     * Clear the current authentication context.
     */
    public static void clearContext() {
        context.remove();
    }

    /**
     * Check if the current context is authenticated
     */
    public boolean isAuthenticated() {
        AuthenticationContext authContext = getContext();
        if (authContext == null) {
            return false;
        }

        if (authContext.getAccessToken() == null) {
            logger.warn("Context is not authenticated");
            return false;
        }

        if (authContext.isExpired()) {
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
            logger.warn("Attempting to get access token from unauthenticated context");
            return null;
        }
        return getContext().getAccessToken();
    }
}
