package org.springframework.ai.mcp.sample.server.transport;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.AuthService;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.ai.mcp.sample.server.security.AuthenticationContext;
import org.springframework.ai.mcp.sample.server.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST controller for MCP Server-Sent Events transport.
 * Handles HTTP-based MCP communication with OAuth2 authentication.
 */
@RestController
@RequestMapping("${mcp.server.sse.path:/mcp}")
@ConditionalOnProperty(name = "mcp.server.sse.enabled", havingValue = "true")
public class SseController {

    private static final Logger logger = LoggerFactory.getLogger(SseController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicLong sessionIdGenerator = new AtomicLong(1);

    @Autowired
    private McpServerProperties properties;

    @Autowired
    private SecurityContext securityContext;

    @Autowired
    private AuthService authService;

    @Autowired
    private McpToolIntegration toolIntegration;

    // Active SSE connections
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "transport", "SSE",
            "activeConnections", activeConnections.size(),
            "maxConnections", properties.getSse().getMaxConnections()
        );
        return ResponseEntity.ok(health);
    }

    /**
     * Server information endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = Map.of(
            "name", properties.getName(),
            "version", properties.getVersion(),
            "transport", "SSE",
            "authMode", properties.getAuth().getMode().toString(),
            "capabilities", Map.of(
                "resourceChangeNotification", properties.isResourceChangeNotification(),
                "toolChangeNotification", properties.isToolChangeNotification(),
                "promptChangeNotification", properties.isPromptChangeNotification()
            )
        );
        return ResponseEntity.ok(info);
    }

    /**
     * OAuth2 connection endpoint - establishes SSE connection after authentication
     */
    @GetMapping("/connect")
    public SseEmitter connect(
            Authentication authentication,
            @RegisteredOAuth2AuthorizedClient("mcp-client") OAuth2AuthorizedClient authorizedClient) {
        
        String sessionId = "sse-" + sessionIdGenerator.getAndIncrement();
        logger.info("New SSE connection request: sessionId={}, user={}", 
            sessionId, authentication != null ? authentication.getName() : "anonymous");

        // Check connection limits
        if (activeConnections.size() >= properties.getSse().getMaxConnections()) {
            logger.warn("Maximum connections reached: {}", properties.getSse().getMaxConnections());
            throw new RuntimeException("Maximum connections reached");
        }

        // Create SSE emitter
        long timeoutMs = properties.getSse().getConnectionTimeoutSeconds() * 1000;
        SseEmitter emitter = new SseEmitter(timeoutMs);

        // Set up authentication context
        if (properties.getAuth().getMode() == McpServerProperties.AuthMode.OAUTH2) {
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                long expiresAt = accessToken.getExpiresAt() != null ? 
                    accessToken.getExpiresAt().toEpochMilli() : 
                    System.currentTimeMillis() + Duration.ofHours(1).toMillis();
                
                AuthenticationContext authContext = SecurityContext.createSseContext(
                    sessionId, accessToken.getTokenValue(), expiresAt);
                securityContext.setAuthenticationContext(authContext);
            }
        } else {
            // No auth mode - create basic SSE context
            AuthenticationContext authContext = new AuthenticationContext(TransportMode.SSE, sessionId, null, 0);
            securityContext.setAuthenticationContext(authContext);
        }

        // Store connection
        activeConnections.put(sessionId, emitter);

        // Set up cleanup handlers
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed: sessionId={}", sessionId);
            activeConnections.remove(sessionId);
            securityContext.clear();
        });

        emitter.onTimeout(() -> {
            logger.info("SSE connection timed out: sessionId={}", sessionId);
            activeConnections.remove(sessionId);
            securityContext.clear();
        });

        emitter.onError((ex) -> {
            logger.error("SSE connection error: sessionId={}", sessionId, ex);
            activeConnections.remove(sessionId);
            securityContext.clear();
        });

        // Send initial connection message
        try {
            Map<String, Object> welcomeMessage = Map.of(
                "type", "connection",
                "sessionId", sessionId,
                "server", Map.of(
                    "name", properties.getName(),
                    "version", properties.getVersion()
                ),
                "timestamp", System.currentTimeMillis()
            );
            emitter.send(SseEmitter.event()
                .name("mcp-connection")
                .data(objectMapper.writeValueAsString(welcomeMessage))
                .id(sessionId + "-0"));
        } catch (IOException e) {
            logger.error("Failed to send welcome message: sessionId={}", sessionId, e);
            emitter.completeWithError(e);
        }

        logger.info("SSE connection established: sessionId={}", sessionId);
        return emitter;
    }

    /**
     * Handle MCP requests via POST
     */
    @PostMapping(value = "/request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleMcpRequest(
            @RequestBody JsonNode request,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {

        logger.debug("Received MCP request: sessionId={}, method={}", 
            sessionId, request.has("method") ? request.get("method").asText() : "unknown");

        try {
            // Set up security context for the request
            setupRequestSecurityContext(sessionId, authentication);

            // Process the MCP request
            Map<String, Object> response = processMcpRequest(request);
            
            // Send response via SSE if session is active
            if (sessionId != null && activeConnections.containsKey(sessionId)) {
                sendSseResponse(sessionId, response);
            }

            return ResponseEntity.ok(response);

        } catch (SecurityContext.UnauthorizedException e) {
            logger.warn("Unauthorized MCP request: sessionId={}", sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing MCP request: sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal Server Error", "message", e.getMessage()));
        } finally {
            securityContext.clear();
        }
    }

    /**
     * Set up security context for the current request
     */
    private void setupRequestSecurityContext(String sessionId, Authentication authentication) {
        if (properties.getAuth().getMode() == McpServerProperties.AuthMode.OAUTH2) {
            securityContext.requireAuthentication();
        } else if (sessionId != null) {
            // Create basic SSE context for the request
            AuthenticationContext authContext = new AuthenticationContext(TransportMode.SSE, sessionId, null, 0);
            securityContext.setAuthenticationContext(authContext);
        }
    }

    /**
     * Process MCP request and route to appropriate service
     */
    private Map<String, Object> processMcpRequest(JsonNode request) throws Exception {
        String method = request.has("method") ? request.get("method").asText() : "";
        JsonNode params = request.has("params") ? request.get("params") : objectMapper.createObjectNode();

        switch (method) {
            case "tools/list":
                return Map.of("tools", toolIntegration.getAvailableTools());
            
            case "tools/call":
                return handleToolCall(params);
            
            case "resources/list":
                return Map.of("resources", toolIntegration.getAvailableResources());
            
            case "prompts/list":
                return Map.of("prompts", toolIntegration.getAvailablePrompts());
            
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    /**
     * Handle tool calls
     */
    private Map<String, Object> handleToolCall(JsonNode params) throws Exception {
        String toolName = params.has("name") ? params.get("name").asText() : "";
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        // Use the tool integration service to invoke the tool
        return toolIntegration.invokeTool(toolName, arguments);
    }


    /**
     * Send response via SSE
     */
    private void sendSseResponse(String sessionId, Map<String, Object> response) {
        SseEmitter emitter = activeConnections.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("mcp-response")
                    .data(objectMapper.writeValueAsString(response))
                    .id(sessionId + "-" + System.currentTimeMillis()));
            } catch (IOException e) {
                logger.error("Failed to send SSE response: sessionId={}", sessionId, e);
                activeConnections.remove(sessionId);
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * Get active connections count
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Object>> getConnections() {
        return ResponseEntity.ok(Map.of(
            "activeConnections", activeConnections.size(),
            "maxConnections", properties.getSse().getMaxConnections(),
            "sessions", activeConnections.keySet()
        ));
    }
}