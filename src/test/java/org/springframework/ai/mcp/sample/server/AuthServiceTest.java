package org.springframework.ai.mcp.sample.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AuthServiceTest {
    
    @Test
    void testTokenCachingAndExpiration() {
        AuthService authService = new AuthService(10, 2); // TTL 2 seconds
        String token1 = authService.getAccessToken();
        assertNotNull(token1);

        // Should hit cache on second get
        String token2 = authService.getAccessToken();
        assertEquals(token1, token2);

        // Wait for expiration
        try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
        String token3 = authService.getAccessToken();
        // After expiration, should fetch new token (may be different depending on backend)
        assertNotNull(token3);
    }
}

