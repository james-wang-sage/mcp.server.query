package org.springframework.ai.mcp.sample.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider intacctModelTools(ModelService modelService) {
		// Exposes methods annotated with @Tool in ModelService
		return MethodToolCallbackProvider.builder().toolObjects(modelService).build();
	}

	@Bean
	public ToolCallbackProvider intacctQueryTools(QueryService queryService) {
		// Exposes methods annotated with @Tool in QueryService
		return MethodToolCallbackProvider.builder().toolObjects(queryService).build();
	}

}
