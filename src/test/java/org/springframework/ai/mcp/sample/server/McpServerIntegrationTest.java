package org.springframework.ai.mcp.sample.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.ai.mcp.sample.server.transport.McpToolIntegration;
import org.springframework.ai.mcp.sample.server.transport.TransportManager;
import org.springframework.ai.mcp.sample.server.transport.TransportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for the complete MCP server implementation.
 * Tests the interaction between all components in different modes.
 */
@SpringBootTest
@ActiveProfiles("test") // Use test profile for testing (with mocked auth)
public class McpServerIntegrationTest {

    @Autowired
    private McpServerProperties properties;

    @Autowired
    private TransportManager transportManager;

    @Autowired
    private McpToolIntegration toolIntegration;

    @MockBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Mock the AuthService to return a test token
        when(authService.getAccessToken()).thenReturn("test-access-token");
    }

    @Test
    public void testServerConfiguration() {
        assertNotNull(properties);
        assertEquals("query-server", properties.getName());
        assertEquals("0.1.0", properties.getVersion());
    }

    @Test
    public void testTransportManagerInitialization() {
        assertNotNull(transportManager);
        assertTrue(transportManager.isInitialized());
        
        // In dev profile, SSE should be enabled
        assertTrue(transportManager.isTransportActive(TransportMode.SSE));
        
        // STDIO should be disabled in dev profile
        assertFalse(transportManager.isTransportActive(TransportMode.STDIO));
    }

    @Test
    public void testSseConfiguration() {
        assertTrue(properties.getSse().isEnabled());
        assertEquals(8080, properties.getSse().getPort());
        assertEquals("/mcp", properties.getSse().getPath());
        assertEquals(McpServerProperties.AuthMode.NONE, properties.getAuth().getMode());
    }

    @Test
    public void testToolIntegration() {
        assertNotNull(toolIntegration);
        
        // Test that tools are available
        var tools = toolIntegration.getAvailableTools();
        assertNotNull(tools);
        assertFalse(tools.isEmpty());
        
        // Test that resources are available
        var resources = toolIntegration.getAvailableResources();
        assertNotNull(resources);
        assertFalse(resources.isEmpty());
        
        // Test that prompts are available
        var prompts = toolIntegration.getAvailablePrompts();
        assertNotNull(prompts);
        assertFalse(prompts.isEmpty());
    }

    @Test
    public void testCorsConfiguration() {
        var corsConfig = properties.getSse().getCors();
        assertNotNull(corsConfig);
        assertTrue(corsConfig.isAllowCredentials());
        assertEquals(3600, corsConfig.getMaxAge());
        
        // In dev profile, should allow localhost origins
        String[] allowedOrigins = corsConfig.getAllowedOrigins();
        assertNotNull(allowedOrigins);
        assertTrue(allowedOrigins.length > 0);
    }

    @Test
    public void testSessionConfiguration() {
        var sessionConfig = properties.getAuth().getSession();
        assertNotNull(sessionConfig);
        assertEquals(3600, sessionConfig.getTimeoutSeconds());
        assertEquals(1000, sessionConfig.getMaxSessions());
        assertEquals(300, sessionConfig.getCleanupIntervalSeconds());
    }

    @Test
    public void testServerCapabilities() {
        assertTrue(properties.isResourceChangeNotification());
        assertTrue(properties.isToolChangeNotification());
        assertTrue(properties.isPromptChangeNotification());
    }
}