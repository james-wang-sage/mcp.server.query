package org.springframework.ai.mcp.sample.server.security;

import org.springframework.ai.mcp.sample.server.transport.TransportMode;

/**
 * Holds authentication context information for the current request/session.
 * This includes transport mode, session information, and authentication state.
 */
public class AuthenticationContext {
    
    private final TransportMode transportMode;
    private final String sessionId;
    private final String accessToken;
    private final boolean authenticated;
    private final long expiresAt;
    
    /**
     * Constructor for STDIO mode (no authentication)
     */
    public AuthenticationContext(TransportMode transportMode) {
        this.transportMode = transportMode;
        this.sessionId = null;
        this.accessToken = null;
        this.authenticated = false;
        this.expiresAt = 0;
    }
    
    /**
     * Constructor for SSE mode with OAuth2 authentication
     */
    public AuthenticationContext(TransportMode transportMode, String sessionId, 
                                String accessToken, long expiresAt) {
        this.transportMode = transportMode;
        this.sessionId = sessionId;
        this.accessToken = accessToken;
        this.authenticated = true;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Get the transport mode for this context
     */
    public TransportMode getTransportMode() {
        return transportMode;
    }
    
    /**
     * Get the session ID (SSE mode only)
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Get the access token for API calls
     */
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * Check if the current context is authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Get the token expiration timestamp
     */
    public long getExpiresAt() {
        return expiresAt;
    }
    
    /**
     * Check if the token is expired
     */
    public boolean isExpired() {
        if (!authenticated) {
            return false; // STDIO mode doesn't expire
        }
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * Check if this is STDIO transport mode
     */
    public boolean isStdioMode() {
        return transportMode == TransportMode.STDIO;
    }
    
    /**
     * Check if this is SSE transport mode
     */
    public boolean isSseMode() {
        return transportMode == TransportMode.SSE;
    }
    
    @Override
    public String toString() {
        return "AuthenticationContext{" +
                "transportMode=" + transportMode +
                ", sessionId='" + sessionId + '\'' +
                ", authenticated=" + authenticated +
                ", expired=" + isExpired() +
                '}';
    }
}