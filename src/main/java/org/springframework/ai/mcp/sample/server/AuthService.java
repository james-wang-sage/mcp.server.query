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

    // TODO: Externalize these credentials using Spring configuration properties or environment variables
    private static final String TOKEN_ENDPOINT = "https://partner.intacct.com/ia3/api/v1-beta2/oauth2/token";
    private static final String CLIENT_ID = "d4f2b6b318174b9a60a7.INTACCT.app.sage.com"; // Replace if needed
    private static final String CLIENT_SECRET = "0f4e72b76e88906255c34a800b5e177fce5f1ba9"; // Replace if needed
    private static final String USERNAME = "Admin@Artur-Test2"; // Replace if needed
    private static final String PASSWORD = "Aa123456!"; // Replace if needed

    // Cache expiration buffer (fetch token slightly before it actually expires)
    private static final Duration EXPIRATION_BUFFER = Duration.ofSeconds(60);

    private final RestClient tokenClient;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Caffeine cache for access token
    private final Cache<String, TokenCacheValue> tokenCache;

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
            @Value("${caffeine.auth.cache.ttl-seconds:3300}") int ttlSeconds
    ) {
        this.tokenClient = RestClient.builder().build();
        this.tokenCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .build();
    }

    public AuthService() {
        this(10, 3300); // 默认最大容量10，TTL 3300秒
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
        TokenRequest tokenRequest = new TokenRequest(
                "password",
                CLIENT_ID,
                CLIENT_SECRET,
                USERNAME,
                PASSWORD
        );
        try {
            TokenResponse response = this.tokenClient.post()
                    .uri(TOKEN_ENDPOINT)
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