package org.springframework.ai.mcp.sample.server;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.annotation.PostConstruct;

/**
 * Handles OAuth2 authentication (password grant) for Intacct API.
 * Includes token caching and basic expiration handling.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Cache expiration buffer (fetch token slightly before it actually expires)
    private static final Duration EXPIRATION_BUFFER = Duration.ofSeconds(60);

    private final RestClient tokenClient;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Caffeine cache for access token
    private final Cache<String, TokenCacheValue> tokenCache;

    // Intacct OAuth2 configuration loaded from system properties or application properties
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;

    // --- DTOs for OAuth2 Token Exchange ---
    private record TokenRequest(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            String username,
            String password
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Integer expiresIn
            // Add other fields if needed (e.g., refresh_token)
    ) {}

    public AuthService(
            @Value("${caffeine.auth.cache.max-size:10}") int maxSize,
            @Value("${caffeine.auth.cache.ttl-seconds:3300}") int ttlSeconds,
            @Value("${intacct.base-url:https://partner.intacct.com/ia3/api/v1-beta2}") String baseUrl,
            @Value("${intacct.client-id:}") String clientId,
            @Value("${intacct.client-secret:}") String clientSecret,
            @Value("${intacct.username:}") String username,
            @Value("${intacct.password:}") String password
    ) {
        this.tokenClient = RestClient.builder().build();
        this.tokenCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .build();
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
    }

    public AuthService() {
        this(10, 3300,
            System.getProperty("intacct.base-url", "https://partner.intacct.com/ia3/api/v1-beta2"),
            System.getProperty("intacct.client-id", ""),
            System.getProperty("intacct.client-secret", ""),
            System.getProperty("intacct.username", ""),
            System.getProperty("intacct.password", "")
        );
    }

    /**
     * Gets a valid access token from Caffeine cache, fetching a new one if needed.
     * This method is thread-safe.
     *
     * @return A valid access token, or null if fetching fails.
     */
    public String getAccessToken() {
        tokenLock.lock();
        try {
            TokenCacheValue cacheValue = tokenCache.getIfPresent("access_token");
            if (cacheValue != null && Instant.now().isBefore(cacheValue.expiration().minus(EXPIRATION_BUFFER))) {
                logger.debug("Returning cached access token from Caffeine.");
                return cacheValue.token();
            }
            logger.info("Cached token is null or expired. Fetching new access token...");
            boolean success = fetchNewAccessToken();
            if (success) {
                TokenCacheValue newValue = tokenCache.getIfPresent("access_token");
                return newValue != null ? newValue.token() : null;
            } else {
                tokenCache.invalidate("access_token");
                return null;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Fetches a new access token from the provider and updates the Caffeine cache.
     *
     * @return true if fetching and caching was successful, false otherwise.
     */
    private boolean fetchNewAccessToken() {
        logger.info("Calling Intacct token endpoint...");
        String tokenEndpoint = System.getProperty("intacct.token.endpoint");
        if (tokenEndpoint == null || tokenEndpoint.isEmpty()) {
            tokenEndpoint = this.baseUrl + "/oauth2/token";
        }
        TokenRequest tokenRequest = new TokenRequest(
                "password",
                this.clientId,
                this.clientSecret,
                this.username,
                this.password
        );
        try {
            TokenResponse response = this.tokenClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(tokenRequest)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response != null && response.accessToken() != null && response.expiresIn() != null) {
                Instant expiration = Instant.now().plusSeconds(response.expiresIn());
                tokenCache.put("access_token", new TokenCacheValue(response.accessToken(), expiration));
                logger.info("Successfully obtained and cached new OAuth2 access token (Caffeine). Expires around: {}", expiration);
                return true;
            } else {
                logger.error("Received null or invalid response from token endpoint. Response: {}", response);
                return false;
            }
        } catch (RestClientException e) {
            logger.error("Error fetching new access token: {}", e.getMessage(), e);
            return false;
        }
    }

    // TokenCacheValue: holds token and expiration
    private record TokenCacheValue(String token, Instant expiration) {}

    // Optional: Initial fetch on startup (if needed immediately)
    @PostConstruct
    public void init() {
        getAccessToken(); // Fetch token eagerly on application startup
    }
}