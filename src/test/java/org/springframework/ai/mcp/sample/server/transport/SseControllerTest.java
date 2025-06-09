package org.springframework.ai.mcp.sample.server.transport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for SSE Controller functionality.
 * Tests the HTTP endpoints for MCP over SSE transport.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("dev") // Use dev profile for testing (no auth)
public class SseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/mcp/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.transport").value("SSE"));
    }

    @Test
    public void testInfoEndpoint() throws Exception {
        mockMvc.perform(get("/mcp/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("query-server"))
            .andExpect(jsonPath("$.transport").value("SSE"))
            .andExpect(jsonPath("$.authMode").value("NONE"));
    }

    @Test
    public void testConnectionsEndpoint() throws Exception {
        mockMvc.perform(get("/mcp/connections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeConnections").isNumber())
            .andExpect(jsonPath("$.maxConnections").isNumber());
    }

    @Test
    public void testToolsListRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            java.util.Map.of("method", "tools/list")
        );

        mockMvc.perform(post("/mcp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tools").exists());
    }

    @Test
    public void testResourcesListRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            java.util.Map.of("method", "resources/list")
        );

        mockMvc.perform(post("/mcp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resources").exists());
    }

    @Test
    public void testPromptsListRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            java.util.Map.of("method", "prompts/list")
        );

        mockMvc.perform(post("/mcp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.prompts").exists());
    }

    @Test
    public void testUnknownMethodRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            java.util.Map.of("method", "unknown/method")
        );

        mockMvc.perform(post("/mcp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    public void testToolCallRequest() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            java.util.Map.of(
                "method", "tools/call",
                "params", java.util.Map.of(
                    "name", "query_testTool",
                    "arguments", java.util.Map.of("param0", "test")
                )
            )
        );

        mockMvc.perform(post("/mcp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        // Note: The actual response depends on the tool implementation
    }
}