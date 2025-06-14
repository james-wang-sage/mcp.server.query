package org.springframework.ai.mcp.sample.server.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for MCP Server supporting STDIO transport mode.
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
     * Authentication configuration
     */
    @NestedConfigurationProperty
    private AuthConfig auth = new AuthConfig();

    /**
     * Tool integration configuration
     */
    @NestedConfigurationProperty
    private ToolIntegrationConfig toolIntegration = new ToolIntegrationConfig();

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

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }

    public ToolIntegrationConfig getToolIntegration() {
        return toolIntegration;
    }

    public void setToolIntegration(ToolIntegrationConfig toolIntegration) {
        this.toolIntegration = toolIntegration;
    }

    /**
     * Server type enumeration
     */
    public enum ServerType {
        SYNC,
        ASYNC
    }

    /**
     * Authentication mode enumeration
     */
    public enum AuthMode {
        NONE,
        OAUTH2,
        BASIC
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
     * Authentication configuration
     */
    public static class AuthConfig {
        /**
         * Authentication mode
         */
        private AuthMode mode = AuthMode.NONE;

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

        // 代理方法，便于兼容 AuthService 代码
        public String getBaseUrl() {
            return oauth2.getBaseUrl();
        }
        public String getUsername() {
            return oauth2.getUsername();
        }
        public String getPassword() {
            return oauth2.getPassword();
        }
    }

    /**
     * OAuth2 configuration
     */
    public static class OAuth2Config {
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String redirectUri;
        private String[] scopes = new String[0];
        // 新增字段
        private String baseUrl;
        private String username;
        private String password;

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

        public String getBaseUrl() {
            return baseUrl;
        }
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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
    }

    /**
     * Session configuration
     */
    public static class SessionConfig {
        private long timeoutSeconds = 3600;
        private int maxSessions = 1000;
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
     * Tool integration configuration
     */
    public static class ToolIntegrationConfig {
        private List<String> tools = new ArrayList<>();
        private List<String> resources = new ArrayList<>();
        private List<String> prompts = new ArrayList<>();

        public List<String> getTools() {
            return tools;
        }

        public void setTools(List<String> tools) {
            this.tools = tools;
        }

        public List<String> getResources() {
            return resources;
        }

        public void setResources(List<String> resources) {
            this.resources = resources;
        }

        public List<String> getPrompts() {
            return prompts;
        }

        public void setPrompts(List<String> prompts) {
            this.prompts = prompts;
        }
    }
}