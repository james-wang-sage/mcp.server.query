# MCP Server with SSE and OAuth2 Support

This document describes the enhanced MCP (Model Context Protocol) server implementation that supports both STDIO and SSE (Server-Sent Events) transport modes with OAuth2 authentication.

## Features

### Transport Modes

1. **STDIO Mode** (Default)
   - Traditional process-based communication
   - Uses stdin/stdout for MCP protocol
   - No authentication required
   - Backward compatible with existing MCP clients

2. **SSE Mode** (New)
   - HTTP-based communication using Server-Sent Events
   - REST endpoints for MCP operations
   - OAuth2 authentication support
   - CORS configuration for web clients
   - Session management

3. **Dual Mode**
   - Both STDIO and SSE transports active simultaneously
   - Useful for development and testing

### Authentication

- **None**: No authentication required (default for STDIO, optional for SSE)
- **OAuth2**: Full OAuth2 authorization code grant flow for SSE mode
- **Basic**: HTTP Basic authentication (placeholder for future implementation)

## Configuration

### Application Profiles

The server supports multiple Spring profiles:

- `stdio` (default): STDIO mode only, no web server
- `sse`: SSE mode only with web server
- `dual`: Both STDIO and SSE modes active
- `dev`: Development mode with SSE but no authentication

### Configuration Properties

```yaml
mcp:
  server:
    name: query-server
    version: 0.1.0
    
    # STDIO Configuration
    stdio:
      enabled: true
      disable-console-logging: true
      disable-banner: true
    
    # SSE Configuration
    sse:
      enabled: false
      port: 8080
      path: /mcp
      connection-timeout-seconds: 300
      max-connections: 100
      cors:
        allowed-origins: ["*"]
        allowed-methods: ["GET", "POST", "OPTIONS"]
        allowed-headers: ["*"]
        allow-credentials: true
        max-age: 3600
    
    # Authentication Configuration
    auth:
      mode: NONE  # NONE, OAUTH2, BASIC
      oauth2:
        client-id: ${OAUTH2_CLIENT_ID:}
        client-secret: ${OAUTH2_CLIENT_SECRET:}
        authorization-uri: ${OAUTH2_AUTH_URI:}
        token-uri: ${OAUTH2_TOKEN_URI:}
        redirect-uri: ${OAUTH2_REDIRECT_URI:}
        scopes: []
      session:
        timeout-seconds: 3600
        max-sessions: 1000
        cleanup-interval-seconds: 300
```

### Environment Variables

For OAuth2 configuration, set these environment variables:

```bash
OAUTH2_CLIENT_ID=your-client-id
OAUTH2_CLIENT_SECRET=your-client-secret
OAUTH2_AUTH_URI=https://provider.com/oauth2/authorize
OAUTH2_TOKEN_URI=https://provider.com/oauth2/token
OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/mcp-client
```

## Running the Server

### STDIO Mode (Default)

```bash
# Run with default STDIO profile
java -jar mcp-query-stdio-server-0.1.0.jar

# Or explicitly specify STDIO profile
java -jar mcp-query-stdio-server-0.1.0.jar --spring.profiles.active=stdio
```

### SSE Mode with OAuth2

```bash
# Set environment variables
export OAUTH2_CLIENT_ID=your-client-id
export OAUTH2_CLIENT_SECRET=your-client-secret
export OAUTH2_AUTH_URI=https://provider.com/oauth2/authorize
export OAUTH2_TOKEN_URI=https://provider.com/oauth2/token
export OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/mcp-client

# Run with SSE profile
java -jar mcp-query-stdio-server-0.1.0.jar --spring.profiles.active=sse
```

### Development Mode (SSE without Auth)

```bash
java -jar mcp-query-stdio-server-0.1.0.jar --spring.profiles.active=dev
```

### Dual Mode

```bash
java -jar mcp-query-stdio-server-0.1.0.jar --spring.profiles.active=dual
```

## SSE API Endpoints

When running in SSE mode, the following HTTP endpoints are available:

### Public Endpoints

- `GET /mcp/health` - Health check
- `GET /mcp/info` - Server information
- `GET /mcp/connections` - Active connections info

### Authentication Endpoints (OAuth2 mode)

- `GET /oauth2/authorization/mcp-client` - Start OAuth2 flow
- `GET /login/oauth2/code/mcp-client` - OAuth2 callback

### MCP Protocol Endpoints

- `GET /mcp/connect` - Establish SSE connection (requires auth in OAuth2 mode)
- `POST /mcp/request` - Send MCP requests

### Example Usage

#### 1. Health Check

```bash
curl http://localhost:8080/mcp/health
```

Response:
```json
{
  "status": "UP",
  "transport": "SSE",
  "activeConnections": 0,
  "maxConnections": 100
}
```

#### 2. Server Info

```bash
curl http://localhost:8080/mcp/info
```

Response:
```json
{
  "name": "query-server",
  "version": "0.1.0",
  "transport": "SSE",
  "authMode": "OAUTH2",
  "capabilities": {
    "resourceChangeNotification": true,
    "toolChangeNotification": true,
    "promptChangeNotification": true
  }
}
```

#### 3. List Available Tools

```bash
curl -X POST http://localhost:8080/mcp/request \
  -H "Content-Type: application/json" \
  -d '{"method": "tools/list"}'
```

#### 4. Call a Tool

```bash
curl -X POST http://localhost:8080/mcp/request \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tools/call",
    "params": {
      "name": "query_searchData",
      "arguments": {
        "query": "example search"
      }
    }
  }'
```

## OAuth2 Flow

1. **Authorization**: Client redirects user to `/oauth2/authorization/mcp-client`
2. **Callback**: OAuth2 provider redirects back to `/login/oauth2/code/mcp-client`
3. **Connection**: After successful auth, client can connect to `/mcp/connect`
4. **SSE Stream**: Server establishes SSE connection with session management
5. **API Calls**: Client can make authenticated requests to `/mcp/request`

## Security Features

- **CORS Support**: Configurable cross-origin resource sharing
- **Session Management**: Automatic session cleanup and timeout handling
- **Token Validation**: OAuth2 access token validation and refresh
- **Connection Limits**: Configurable maximum concurrent connections
- **Request Authentication**: Per-request authentication context

## Development

### Building

```bash
mvn clean package
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=SseControllerTest
```

### Profiles for Development

- Use `dev` profile for local development without OAuth2
- Use `dual` profile to test both STDIO and SSE simultaneously
- Set `mcp.server.sse.cors.allowed-origins` to specific origins in production

## Architecture

### Key Components

1. **McpServerApplication**: Main application with profile-based startup
2. **TransportManager**: Manages lifecycle of transport modes
3. **OAuth2SecurityConfig**: Spring Security configuration for OAuth2
4. **SseController**: REST endpoints for SSE transport
5. **McpToolIntegration**: Bridges MCP tools with HTTP transport
6. **SessionManager**: Manages OAuth2 sessions and cleanup
7. **SecurityContext**: Thread-local authentication context

### Transport Abstraction

The server uses a transport abstraction that allows:
- Unified tool and resource access across transport modes
- Transport-specific authentication handling
- Seamless switching between modes via configuration

## Troubleshooting

### Common Issues

1. **OAuth2 Configuration**: Ensure all OAuth2 environment variables are set
2. **CORS Issues**: Check `allowed-origins` configuration for web clients
3. **Connection Limits**: Monitor active connections via `/mcp/connections`
4. **Session Cleanup**: Check logs for session management issues

### Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    org.springframework.ai.mcp.sample.server: DEBUG
    org.springframework.security: DEBUG
```

## Future Enhancements

- WebSocket transport support
- Additional authentication methods (JWT, API keys)
- Metrics and monitoring endpoints
- Rate limiting and throttling
- Multi-tenant support