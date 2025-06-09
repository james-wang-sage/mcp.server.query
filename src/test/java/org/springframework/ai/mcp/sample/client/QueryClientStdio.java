package org.springframework.ai.mcp.sample.client;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * Test client for STDIO mode with query functionality
 */
public class QueryClientStdio {

    public static void main(String[] args) {
        // Build server parameters
        var stdioParams = ServerParameters.builder("java")
            .args("-jar",
                  "target/mcp-query-stdio-server-0.1.0.jar")
            .build();

        // Initialize transport and client
        var transport = new StdioClientTransport(stdioParams);
        var client = McpClient.sync(transport).build();

        try {
            // Initialize the client
            client.initialize();

            // List available tools
            ListToolsResult toolsList = client.listTools();
            System.out.println("Available Tools = " + toolsList);

            // Test query execution
            CallToolResult queryResult = client.callTool(new CallToolRequest("executeQuery",
                Map.of(
                    "object", "accounts-payable/vendor",
                    "fields", List.of("id", "name", "status"),
                    "filters", List.of(Map.of("$eq", Map.of("status", "active"))),
                    "size", 5
                )
            ));
            System.out.println("Query Result: " + queryResult);

        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always close the client gracefully
            client.closeGracefully();
        }
    }
} 