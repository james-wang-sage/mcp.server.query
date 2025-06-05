package org.springframework.ai.mcp.sample.server.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.sample.server.config.McpServerProperties;
import org.springframework.ai.mcp.sample.server.transport.TransportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Manages user sessions for SSE transport mode with OAuth2 authentication.
 * Handles session creation, validation, cleanup, and token refresh.
 */
@Service
@ConditionalOnProperty(name = "mcp.server.sse.enabled", havingValue = "true")
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    @Autowired
    private McpServerProperties properties;

    // Active sessions storage
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void initialize() {
        logger.info("Initializing SessionManager for SSE transport");
        
        // Start cleanup scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        long cleanupInterval = properties.getAuth().getSession().getCleanupIntervalSeconds();
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 
            cleanupInterval, cleanupInterval, TimeUnit.SECONDS);
        
        logger.info("SessionManager initialized with cleanup interval: {}s", cleanupInterval);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down SessionManager");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        activeSessions.clear();
        logger.info("SessionManager shutdown complete");
    }

    /**
     * Create a new session
     */
    public SessionInfo createSession(String sessionId, String userId, String accessToken, long expiresAt) {
        if (activeSessions.size() >= properties.getAuth().getSession().getMaxSessions()) {
            throw new IllegalStateException("Maximum sessions reached: " + 
                properties.getAuth().getSession().getMaxSessions());
        }

        SessionInfo session = new SessionInfo(sessionId, userId, accessToken, expiresAt);
        activeSessions.put(sessionId, session);
        
        logger.info("Created session: sessionId={}, userId={}, expiresAt={}", 
            sessionId, userId, Instant.ofEpochMilli(expiresAt));
        
        return session;
    }

    /**
     * Get session by ID
     */
    public SessionInfo getSession(String sessionId) {
        SessionInfo session = activeSessions.get(sessionId);
        if (session != null && session.isExpired()) {
            logger.info("Removing expired session: sessionId={}", sessionId);
            activeSessions.remove(sessionId);
            return null;
        }
        return session;
    }

    /**
     * Update session access token
     */
    public boolean updateSessionToken(String sessionId, String newAccessToken, long newExpiresAt) {
        SessionInfo session = activeSessions.get(sessionId);
        if (session != null) {
            session.updateToken(newAccessToken, newExpiresAt);
            logger.info("Updated token for session: sessionId={}, newExpiresAt={}", 
                sessionId, Instant.ofEpochMilli(newExpiresAt));
            return true;
        }
        return false;
    }

    /**
     * Remove session
     */
    public boolean removeSession(String sessionId) {
        SessionInfo removed = activeSessions.remove(sessionId);
        if (removed != null) {
            logger.info("Removed session: sessionId={}, userId={}", sessionId, removed.getUserId());
            return true;
        }
        return false;
    }

    /**
     * Check if session exists and is valid
     */
    public boolean isValidSession(String sessionId) {
        SessionInfo session = getSession(sessionId);
        return session != null && !session.isExpired();
    }

    /**
     * Get active sessions count
     */
    public int getActiveSessionsCount() {
        return activeSessions.size();
    }

    /**
     * Get session statistics
     */
    public Map<String, Object> getSessionStats() {
        long expiredCount = activeSessions.values().stream()
            .mapToLong(session -> session.isExpired() ? 1 : 0)
            .sum();

        return Map.of(
            "totalSessions", activeSessions.size(),
            "expiredSessions", expiredCount,
            "maxSessions", properties.getAuth().getSession().getMaxSessions(),
            "timeoutSeconds", properties.getAuth().getSession().getTimeoutSeconds()
        );
    }

    /**
     * Create authentication context from session
     */
    public AuthenticationContext createAuthenticationContext(String sessionId) {
        SessionInfo session = getSession(sessionId);
        if (session == null) {
            return null;
        }
        
        return new AuthenticationContext(
            TransportMode.SSE, 
            sessionId, 
            session.getAccessToken(), 
            session.getExpiresAt()
        );
    }

    /**
     * Cleanup expired sessions
     */
    private void cleanupExpiredSessions() {
        try {
            long beforeCount = activeSessions.size();
            
            activeSessions.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired()) {
                    logger.debug("Cleaning up expired session: sessionId={}", entry.getKey());
                    return true;
                }
                return false;
            });
            
            long afterCount = activeSessions.size();
            long removedCount = beforeCount - afterCount;
            
            if (removedCount > 0) {
                logger.info("Cleaned up {} expired sessions, {} active sessions remaining", 
                    removedCount, afterCount);
            }
            
        } catch (Exception e) {
            logger.error("Error during session cleanup", e);
        }
    }

    /**
     * Session information holder
     */
    public static class SessionInfo {
        private final String sessionId;
        private final String userId;
        private final long createdAt;
        private volatile String accessToken;
        private volatile long expiresAt;
        private volatile long lastAccessedAt;

        public SessionInfo(String sessionId, String userId, String accessToken, long expiresAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessedAt = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getUserId() {
            return userId;
        }

        public String getAccessToken() {
            updateLastAccessed();
            return accessToken;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getLastAccessedAt() {
            return lastAccessedAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        public void updateToken(String newAccessToken, long newExpiresAt) {
            this.accessToken = newAccessToken;
            this.expiresAt = newExpiresAt;
            updateLastAccessed();
        }

        private void updateLastAccessed() {
            this.lastAccessedAt = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "SessionInfo{" +
                    "sessionId='" + sessionId + '\'' +
                    ", userId='" + userId + '\'' +
                    ", createdAt=" + Instant.ofEpochMilli(createdAt) +
                    ", expiresAt=" + Instant.ofEpochMilli(expiresAt) +
                    ", expired=" + isExpired() +
                    '}';
        }
    }
}