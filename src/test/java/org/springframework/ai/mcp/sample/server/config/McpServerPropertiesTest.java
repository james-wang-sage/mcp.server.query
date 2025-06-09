package org.springframework.ai.mcp.sample.server.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {McpServerConfiguration.class})
@EnableConfigurationProperties(McpServerProperties.class)
@TestPropertySource(properties = {
    "mcp.server.name=test-server",
    "mcp.server.version=1.0.0",
    "mcp.server.stdio.enabled=true",
    "mcp.server.sse.enabled=false",
    "mcp.server.sse.port=9090",
    "mcp.server.auth.mode=OAUTH2"
})
class McpServerPropertiesTest {

    @Autowired
    private McpServerProperties properties;

    @Test
    void testBasicProperties() {
        assertEquals("test-server", properties.getName());
        assertEquals("1.0.0", properties.getVersion());
        assertEquals(McpServerProperties.ServerType.SYNC, properties.getType());
    }

    @Test
    void testStdioConfiguration() {
        assertTrue(properties.getStdio().isEnabled());
        assertTrue(properties.getStdio().isDisableConsoleLogging());
        assertTrue(properties.getStdio().isDisableBanner());
    }

    @Test
    void testSseConfiguration() {
        assertFalse(properties.getSse().isEnabled());
        assertEquals(9090, properties.getSse().getPort());
        assertEquals("/mcp", properties.getSse().getPath());
        assertEquals(300, properties.getSse().getConnectionTimeoutSeconds());
        assertEquals(100, properties.getSse().getMaxConnections());
    }

    @Test
    void testAuthConfiguration() {
        assertEquals(McpServerProperties.AuthMode.OAUTH2, properties.getAuth().getMode());
        assertEquals(3600, properties.getAuth().getSession().getTimeoutSeconds());
        assertEquals(1000, properties.getAuth().getSession().getMaxSessions());
        assertEquals(300, properties.getAuth().getSession().getCleanupIntervalSeconds());
    }

    @Test
    void testCorsConfiguration() {
        McpServerProperties.CorsConfig cors = properties.getSse().getCors();
        assertArrayEquals(new String[]{"*"}, cors.getAllowedOrigins());
        assertArrayEquals(new String[]{"GET", "POST", "OPTIONS"}, cors.getAllowedMethods());
        assertArrayEquals(new String[]{"*"}, cors.getAllowedHeaders());
        assertTrue(cors.isAllowCredentials());
        assertEquals(3600, cors.getMaxAge());
    }
}