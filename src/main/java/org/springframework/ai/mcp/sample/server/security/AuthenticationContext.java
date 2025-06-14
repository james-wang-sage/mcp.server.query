package org.springframework.ai.mcp.sample.server.security;

import org.springframework.ai.mcp.sample.server.transport.TransportMode;

/**
 * Represents the authentication context for a user session.
 * Currently only supports STDIO transport mode.
 */
public class AuthenticationContext {
    private final TransportMode transportMode;
    private String accessToken;
    private long expiresAt;

    /**
     * Constructor for STDIO mode
     */
    public AuthenticationContext(TransportMode transportMode) {
        this.transportMode = transportMode;
    }

    /**
     * Get the transport mode
     */
    public TransportMode getTransportMode() {
        return transportMode;
    }

    /**
     * Check if this is STDIO transport mode
     */
    public boolean isStdioMode() {
        return transportMode == TransportMode.STDIO;
    }

    /**
     * Get the access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Set the access token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Get the token expiration time
     */
    public long getExpiresAt() {
        return expiresAt;
    }

    /**
     * Set the token expiration time
     */
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Check if the token is expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    /**
     * Check if the context is authenticated
     */
    public boolean isAuthenticated() {
        return accessToken != null && !isExpired();
    }
}