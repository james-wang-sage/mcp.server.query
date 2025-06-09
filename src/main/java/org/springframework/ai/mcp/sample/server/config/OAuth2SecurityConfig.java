package org.springframework.ai.mcp.sample.server.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth2 security configuration for SSE transport mode.
 * This configuration is only active when SSE mode is enabled and OAuth2 authentication is configured.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "mcp.server.sse.enabled", havingValue = "true")
public class OAuth2SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SecurityConfig.class);

    @Autowired
    private McpServerProperties properties;

    @Autowired
    private SecurityContext securityContext;

    /**
     * Configure HTTP security for OAuth2 authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring OAuth2 security for SSE mode");

        McpServerProperties.AuthMode authMode = properties.getAuth().getMode();
        String basePath = properties.getSse().getPath();

        if (authMode == McpServerProperties.AuthMode.OAUTH2) {
            // OAuth2 configuration
            http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                    // Public endpoints
                    .requestMatchers(basePath + "/health", basePath + "/info").permitAll()
                    .requestMatchers("/oauth2/**", "/login/**").permitAll()
                    // Protected MCP endpoints
                    .requestMatchers(basePath + "/**").authenticated()
                    .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                    .loginPage("/oauth2/authorization/mcp-client")
                    .successHandler(authenticationSuccessHandler())
                    .failureUrl("/login?error=true")
                );
        } else if (authMode == McpServerProperties.AuthMode.NONE) {
            // No authentication required
            http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                    .anyRequest().permitAll()
                );
        } else {
            // Basic auth or other modes
            http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers(basePath + "/health", basePath + "/info").permitAll()
                    .requestMatchers(basePath + "/**").authenticated()
                    .anyRequest().permitAll()
                )
                .httpBasic(basic -> {});
        }

        return http.build();
    }

    /**
     * Configure CORS for SSE endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        McpServerProperties.CorsConfig corsConfig = properties.getSse().getCors();

        configuration.setAllowedOriginPatterns(Arrays.asList(corsConfig.getAllowedOrigins()));
        configuration.setAllowedMethods(Arrays.asList(corsConfig.getAllowedMethods()));
        configuration.setAllowedHeaders(Arrays.asList(corsConfig.getAllowedHeaders()));
        configuration.setAllowCredentials(corsConfig.isAllowCredentials());
        configuration.setMaxAge(corsConfig.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        logger.debug("CORS configuration: origins={}, methods={}, headers={}", 
            Arrays.toString(corsConfig.getAllowedOrigins()),
            Arrays.toString(corsConfig.getAllowedMethods()),
            Arrays.toString(corsConfig.getAllowedHeaders()));

        return source;
    }

    /**
     * Configure OAuth2 client registration
     */
    @Bean
    @ConditionalOnProperty(name = "mcp.server.auth.mode", havingValue = "OAUTH2")
    public ClientRegistrationRepository clientRegistrationRepository() {
        McpServerProperties.OAuth2Config oauth2Config = properties.getAuth().getOauth2();

        if (oauth2Config.getClientId() == null || oauth2Config.getClientId().trim().isEmpty()) {
            throw new IllegalArgumentException("OAuth2 client ID is required when auth mode is OAUTH2");
        }

        ClientRegistration registration = ClientRegistration.withRegistrationId("mcp-client")
            .clientId(oauth2Config.getClientId())
            .clientSecret(oauth2Config.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(oauth2Config.getRedirectUri())
            .scope(oauth2Config.getScopes())
            .authorizationUri(oauth2Config.getAuthorizationUri())
            .tokenUri(oauth2Config.getTokenUri())
            .clientName("MCP Client")
            .build();

        logger.info("OAuth2 client registration configured: clientId={}, redirectUri={}", 
            oauth2Config.getClientId(), oauth2Config.getRedirectUri());

        return new InMemoryClientRegistrationRepository(registration);
    }

    /**
     * Handle successful OAuth2 authentication
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
                // Redirect to the MCP SSE endpoint or a success page
                return properties.getSse().getPath() + "/connect";
            }
        };
    }
}