package org.springframework.ai.mcp.sample.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test network connectivity to Intacct API
 */
public class NetworkConnectivityTest {

    private static final String BASE_URL = "https://partner.intacct.com";
    private static final String API_PATH = "/ia/api/v1-beta2";
    private static final String FULL_URL = BASE_URL + API_PATH;

    @Test
    void testBasicConnectivity() {
        try {
            // Test basic socket connection
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("partner.intacct.com", 443), 10000);
            socket.close();
            System.out.println("✅ Basic socket connection to partner.intacct.com:443 successful");
        } catch (IOException e) {
            System.err.println("❌ Basic socket connection failed: " + e.getMessage());
            fail("Cannot connect to partner.intacct.com:443");
        }
    }

    @Test
    void testHttpConnection() {
        try {
            URL url = new URL(FULL_URL + "/services/core/query");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "MCP-Query-Server/1.0 (Java)");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("✅ HTTP connection successful. Response code: " + responseCode);
            
            // We expect 401 (Unauthorized) since we're not sending auth headers
            assertTrue(responseCode == 401 || responseCode == 200 || responseCode == 405, 
                "Expected 401, 200, or 405, but got: " + responseCode);
            
        } catch (IOException e) {
            System.err.println("❌ HTTP connection failed: " + e.getMessage());
            fail("HTTP connection failed: " + e.getMessage());
        }
    }

    @Test
    void testRestClientConnection() {
        RestClient restClient = RestClient.builder()
                .baseUrl(FULL_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, "MCP-Query-Server/1.0 (Java)")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            // Try to make a request (should fail with 401, but connection should work)
            restClient.get()
                    .uri("/services/core/query")
                    .retrieve()
                    .body(String.class);
            
            System.out.println("✅ RestClient connection successful (unexpected success)");
            
        } catch (RestClientException e) {
            System.out.println("✅ RestClient connection successful (expected auth failure): " + e.getMessage());
            
            // We expect this to fail with authentication error, not connection error
            String message = e.getMessage().toLowerCase();
            assertFalse(message.contains("connection"), 
                "Connection error detected: " + e.getMessage());
            assertFalse(message.contains("timeout"), 
                "Timeout error detected: " + e.getMessage());
            assertFalse(message.contains("unknown host"), 
                "DNS resolution error detected: " + e.getMessage());
        }
    }

    @Test
    void testWithDifferentUserAgents() {
        String[] userAgents = {
            "MCP-Query-Server/1.0 (Java)",
            "Mozilla/5.0 (compatible; MCP-Query-Server/1.0)",
            "curl/7.68.0",
            "Java/17"
        };

        for (String userAgent : userAgents) {
            try {
                URL url = new URL(FULL_URL + "/services/core/query");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", userAgent);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                System.out.println("User-Agent: " + userAgent + " -> Response: " + responseCode);
                
                // All should work (401 is expected without auth)
                assertTrue(responseCode > 0, "No response for User-Agent: " + userAgent);
                
            } catch (IOException e) {
                System.err.println("❌ Failed with User-Agent '" + userAgent + "': " + e.getMessage());
            }
        }
    }

    @Test
    void testSystemProperties() {
        System.out.println("=== System Properties ===");
        System.out.println("java.version: " + System.getProperty("java.version"));
        System.out.println("java.vendor: " + System.getProperty("java.vendor"));
        System.out.println("os.name: " + System.getProperty("os.name"));
        System.out.println("http.proxyHost: " + System.getProperty("http.proxyHost"));
        System.out.println("http.proxyPort: " + System.getProperty("http.proxyPort"));
        System.out.println("https.proxyHost: " + System.getProperty("https.proxyHost"));
        System.out.println("https.proxyPort: " + System.getProperty("https.proxyPort"));
        System.out.println("java.net.useSystemProxies: " + System.getProperty("java.net.useSystemProxies"));
        
        // Check environment variables
        System.out.println("=== Environment Variables ===");
        System.out.println("HTTP_PROXY: " + System.getenv("HTTP_PROXY"));
        System.out.println("HTTPS_PROXY: " + System.getenv("HTTPS_PROXY"));
        System.out.println("NO_PROXY: " + System.getenv("NO_PROXY"));
    }

    @Test
    void testDnsResolution() {
        try {
            InetSocketAddress address = new InetSocketAddress("partner.intacct.com", 443);
            if (address.isUnresolved()) {
                fail("DNS resolution failed for partner.intacct.com");
            } else {
                System.out.println("✅ DNS resolution successful: " + address.getAddress());
            }
        } catch (Exception e) {
            fail("DNS resolution error: " + e.getMessage());
        }
    }
}
