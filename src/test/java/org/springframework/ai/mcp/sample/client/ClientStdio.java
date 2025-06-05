/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
 * With stdio transport, the MCP server is automatically started by the client. But you
 * have to build the server jar first:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 */
public class ClientStdio {

	public static void main(String[] args) {
		// Build server parameters with correct jar path
		var stdioParams = ServerParameters.builder("java")
			.args("-jar",
					"target/mcp-query-stdio-server-0.1.0.jar")
			.build();

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
