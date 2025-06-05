package org.springframework.ai.mcp.sample.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for MCP Server supporting both STDIO and SSE transport modes.
 * This class provides type-safe configuration binding for all MCP server settings.
 */
@ConfigurationProperties(prefix = "mcp.server")
public class McpServerProperties {

    /**
     * Server name identifier
     */
    private String name = "query-server";

    /**
     * Server version
     */
    private String version = "0.1.0";

    /**
     * Server type (SYNC/ASYNC)
     */
    private ServerType type = ServerType.SYNC;

    /**
     * Enable resource change notifications
     */
    private boolean resourceChangeNotification = true;

    /**
     * Enable tool change notifications
     */
    private boolean toolChangeNotification = true;

    /**
     * Enable prompt change notifications
     */
    private boolean promptChangeNotification = true;

    /**
     * STDIO transport configuration
     */
    @NestedConfigurationProperty
    private StdioConfig stdio = new StdioConfig();

    /**
     * SSE transport configuration
     */
    @NestedConfigurationProperty
    private SseConfig sse = new SseConfig();

    /**
     * Authentication configuration
     */
    @NestedConfigurationProperty
    private AuthConfig auth = new AuthConfig();

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ServerType getType() {
        return type;
    }

    public void setType(ServerType type) {
        this.type = type;
    }

    public boolean isResourceChangeNotification() {
        return resourceChangeNotification;
    }

    public void setResourceChangeNotification(boolean resourceChangeNotification) {
        this.resourceChangeNotification = resourceChangeNotification;
    }

    public boolean isToolChangeNotification() {
        return toolChangeNotification;
    }

    public void setToolChangeNotification(boolean toolChangeNotification) {
        this.toolChangeNotification = toolChangeNotification;
    }

    public boolean isPromptChangeNotification() {
        return promptChangeNotification;
    }

    public void setPromptChangeNotification(boolean promptChangeNotification) {
        this.promptChangeNotification = promptChangeNotification;
    }

    public StdioConfig getStdio() {
        return stdio;
    }

    public void setStdio(StdioConfig stdio) {
        this.stdio = stdio;
    }

    public SseConfig getSse() {
        return sse;
    }

    public void setSse(SseConfig sse) {
        this.sse = sse;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }

    /**
     * STDIO transport specific configuration
     */
    public static class StdioConfig {
        /**
         * Enable STDIO transport
         */
        private boolean enabled = true;

        /**
         * Disable console logging for STDIO compatibility
         */
        private boolean disableConsoleLogging = true;

        /**
         * Disable Spring Boot banner for STDIO compatibility
         */
        private boolean disableBanner = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDisableConsoleLogging() {
            return disableConsoleLogging;
        }

        public void setDisableConsoleLogging(boolean disableConsoleLogging) {
            this.disableConsoleLogging = disableConsoleLogging;
        }

        public boolean isDisableBanner() {
            return disableBanner;
        }

        public void setDisableBanner(boolean disableBanner) {
            this.disableBanner = disableBanner;
        }
    }

    /**
     * SSE transport specific configuration
     */
    public static class SseConfig {
        /**
         * Enable SSE transport
         */
        private boolean enabled = false;

        /**
         * Server port for SSE transport
         */
        private int port = 8080;

        /**
         * Base path for SSE endpoints
         */
        private String path = "/mcp";

        /**
         * CORS configuration
         */
        @NestedConfigurationProperty
        private CorsConfig cors = new CorsConfig();

        /**
         * Connection timeout in seconds
         */
        private long connectionTimeoutSeconds = 300;

        /**
         * Maximum number of concurrent connections
         */
        private int maxConnections = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public CorsConfig getCors() {
            return cors;
        }

        public void setCors(CorsConfig cors) {
            this.cors = cors;
        }

        public long getConnectionTimeoutSeconds() {
            return connectionTimeoutSeconds;
        }

        public void setConnectionTimeoutSeconds(long connectionTimeoutSeconds) {
            this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
    }

    /**
     * CORS configuration for SSE transport
     */
    public static class CorsConfig {
        /**
         * Allowed origins for CORS
         */
        private String[] allowedOrigins = {"*"};

        /**
         * Allowed methods for CORS
         */
        private String[] allowedMethods = {"GET", "POST", "OPTIONS"};

        /**
         * Allowed headers for CORS
         */
        private String[] allowedHeaders = {"*"};

        /**
         * Allow credentials in CORS requests
         */
        private boolean allowCredentials = true;

        /**
         * Max age for CORS preflight cache
         */
        private long maxAge = 3600;

        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String[] getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String[] allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String[] getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String[] allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * Authentication configuration
     */
    public static class AuthConfig {
        /**
         * Authentication mode
         */
        private AuthMode mode = AuthMode.NONE;

        /**
         * Username for authentication
         */
        private String username;

        /**
         * Password for authentication
         */
        private String password;

        /**
         * OAuth2 configuration
         */
        @NestedConfigurationProperty
        private OAuth2Config oauth2 = new OAuth2Config();

        /**
         * Session configuration
         */
        @NestedConfigurationProperty
        private SessionConfig session = new SessionConfig();

        public AuthMode getMode() {
            return mode;
        }

        public void setMode(AuthMode mode) {
            this.mode = mode;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public OAuth2Config getOauth2() {
            return oauth2;
        }

        public void setOauth2(OAuth2Config oauth2) {
            this.oauth2 = oauth2;
        }

        public SessionConfig getSession() {
            return session;
        }

        public void setSession(SessionConfig session) {
            this.session = session;
        }
    }

    /**
     * OAuth2 specific configuration
     */
    public static class OAuth2Config {
        /**
         * OAuth2 client ID
         */
        private String clientId;

        /**
         * OAuth2 client secret
         */
        private String clientSecret;

        /**
         * Authorization endpoint URL
         */
        private String authorizationUri;

        /**
         * Token endpoint URL
         */
        private String tokenUri;

        /**
         * Redirect URI for authorization callback
         */
        private String redirectUri;

        /**
         * OAuth2 scopes
         */
        private String[] scopes = {};

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String[] getScopes() {
            return scopes;
        }

        public void setScopes(String[] scopes) {
            this.scopes = scopes;
        }
    }

    /**
     * Session management configuration
     */
    public static class SessionConfig {
        /**
         * Session timeout in seconds
         */
        private long timeoutSeconds = 3600;

        /**
         * Maximum number of concurrent sessions
         */
        private int maxSessions = 1000;

        /**
         * Session cleanup interval in seconds
         */
        private long cleanupIntervalSeconds = 300;

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxSessions() {
            return maxSessions;
        }

        public void setMaxSessions(int maxSessions) {
            this.maxSessions = maxSessions;
        }

        public long getCleanupIntervalSeconds() {
            return cleanupIntervalSeconds;
        }

        public void setCleanupIntervalSeconds(long cleanupIntervalSeconds) {
            this.cleanupIntervalSeconds = cleanupIntervalSeconds;
        }
    }

    /**
     * Server type enumeration
     */
    public enum ServerType {
        SYNC, ASYNC
    }

    /**
     * Authentication mode enumeration
     */
    public enum AuthMode {
        NONE, OAUTH2, BASIC
    }
}