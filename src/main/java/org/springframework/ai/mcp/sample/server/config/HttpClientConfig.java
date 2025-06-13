package org.springframework.ai.mcp.sample.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

/**
 * HTTP client configuration for better network handling
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Set timeouts
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(60));
        
        // Check for proxy configuration
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        
        if (proxyHost != null && proxyPort != null) {
            try {
                int port = Integer.parseInt(proxyPort);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port));
                factory.setProxy(proxy);
                System.out.println("Using HTTP proxy: " + proxyHost + ":" + port);
            } catch (NumberFormatException e) {
                System.err.println("Invalid proxy port: " + proxyPort);
            }
        }
        
        return factory;
    }

    @Bean
    public RestClient.Builder restClientBuilder(ClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "MCP-Query-Server/1.0 (Java)")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Cache-Control", "no-cache");
    }
}
