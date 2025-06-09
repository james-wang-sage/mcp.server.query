package org.springframework.ai.mcp.sample.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.ai.mcp.sample.server.transport.TransportManager;
import org.springframework.ai.mcp.sample.server.transport.TransportMode;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableConfigurationProperties(McpServerProperties.class)
public class McpServerApplication {

	private static final Logger logger = LoggerFactory.getLogger(McpServerApplication.class);

	@Autowired
	private McpServerProperties properties;

	@Autowired
	private TransportManager transportManager;

	@Autowired
	private Environment environment;

	public static void main(String[] args) {
		// Configure application based on active profiles and transport modes
		SpringApplication app = new SpringApplication(McpServerApplication.class);
		
		// Determine if we should run in web mode based on profiles
		String[] activeProfiles = determineActiveProfiles(args);
		boolean isWebMode = shouldRunInWebMode(activeProfiles);
		
		if (!isWebMode) {
			// STDIO mode - disable web application
			app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
			System.setProperty("spring.main.banner-mode", "off");
			System.setProperty("logging.level.root", "OFF");
		}
		
		app.run(args);
	}

	/**
	 * Determine active profiles from command line arguments or environment
	 */
	private static String[] determineActiveProfiles(String[] args) {
		// Check command line arguments for profile
		for (String arg : args) {
			if (arg.startsWith("--spring.profiles.active=")) {
				return arg.substring("--spring.profiles.active=".length()).split(",");
			}
		}
		
		// Check environment variable
		String profilesEnv = System.getProperty("spring.profiles.active");
		if (profilesEnv != null && !profilesEnv.trim().isEmpty()) {
			return profilesEnv.split(",");
		}
		
		// Default to stdio profile
		return new String[]{"stdio"};
	}

	/**
	 * Determine if application should run in web mode
	 */
	private static boolean shouldRunInWebMode(String[] profiles) {
		for (String profile : profiles) {
			String trimmed = profile.trim();
			if ("sse".equals(trimmed) || "dual".equals(trimmed) || "dev".equals(trimmed)) {
				return true;
			}
		}
		return false;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		logger.info("MCP Server '{}' v{} is ready", properties.getName(), properties.getVersion());
		logger.info("Active profiles: {}", String.join(", ", environment.getActiveProfiles()));
		logger.info("Active transports: {}", transportManager.getActiveTransports());
		
		if (transportManager.isTransportActive(TransportMode.STDIO)) {
			logger.info("STDIO transport is active - server ready for process communication");
			if (properties.getStdio().isDisableConsoleLogging()) {
				logger.debug("Console logging disabled for STDIO compatibility");
			}
		}
		
		if (transportManager.isTransportActive(TransportMode.SSE)) {
			logger.info("SSE transport is active on port {} at path {}",
				properties.getSse().getPort(), properties.getSse().getPath());
			logger.info("Authentication mode: {}", properties.getAuth().getMode());
			
			if (properties.getAuth().getMode() == McpServerProperties.AuthMode.OAUTH2) {
				logger.info("OAuth2 client ID: {}", properties.getAuth().getOauth2().getClientId());
				logger.info("OAuth2 redirect URI: {}", properties.getAuth().getOauth2().getRedirectUri());
			}
		}
		
		if (transportManager.isDualModeActive()) {
			logger.info("Dual mode active - both STDIO and SSE transports are available");
		}
		
		// Log tool information
		logger.info("Available tool providers: ModelService, QueryService");
		logger.info("Server capabilities: resourceChangeNotification={}, toolChangeNotification={}, promptChangeNotification={}",
			properties.isResourceChangeNotification(),
			properties.isToolChangeNotification(),
			properties.isPromptChangeNotification());
	}

	@Bean
	public ToolCallbackProvider modelTools(ModelService modelService) {
		// Exposes methods annotated with @Tool in ModelService
		return MethodToolCallbackProvider.builder().toolObjects(modelService).build();
	}

	@Bean
	public ToolCallbackProvider queryTools(QueryService queryService) {
		// Exposes methods annotated with @Tool in QueryService
		return MethodToolCallbackProvider.builder().toolObjects(queryService).build();
	}

}
