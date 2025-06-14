package com.intacct.ds.mcp.server.query.security;

import java.time.Instant;

/**
 * Represents the authentication context for the current request.
 */
public class AuthenticationContext {
    private String accessToken;
    private Instant expiration;
    private String username;
    private String baseUrl;

    public AuthenticationContext() {
        // Default constructor for STDIO mode
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isExpired() {
        return expiration != null && Instant.now().isAfter(expiration);
    }
}
