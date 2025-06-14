# MCP Query Server

A Spring Boot application that implements the MCP (Machine Conversation Protocol) server with STDIO transport mode.

## Features

- MCP Protocol Compliance
- Natural Language to Query API Translation
- OAuth2 Authentication Support
- Error Handling & Logging
- STDIO Transport Mode

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Configuration

The server can be configured through `application.yml` or environment variables:

```yaml
mcp:
  server:
    name: query-server
    version: 0.1.0
    type: SYNC
    resource-change-notification: true
    tool-change-notification: true
    prompt-change-notification: true
    stdio:
      enabled: true
      disable-console-logging: true
      disable-banner: true
    auth:
      mode: NONE
      oauth2:
        client-id: ${OAUTH2_CLIENT_ID}
        client-secret: ${OAUTH2_CLIENT_SECRET}
        authorization-uri: ${OAUTH2_AUTHORIZATION_URI}
        token-uri: ${OAUTH2_TOKEN_URI}
        redirect-uri: ${OAUTH2_REDIRECT_URI}
        scopes:
          - openid
          - profile
          - email
      session:
        timeout-seconds: 3600
        max-sessions: 1000
        cleanup-interval-seconds: 300
```

## Building

```bash
mvn clean package
```

## Running

```bash
java -jar target/mcp-query-server.jar
```

## Development

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── org/
│   │       └── springframework/
│   │           └── ai/
│   │               └── mcp/
│   │                   └── sample/
│   │                       └── server/
│   │                           ├── config/
│   │                           ├── security/
│   │                           └── transport/
│   └── resources/
│       └── application.yml
└── test/
    └── java/
        └── org/
            └── springframework/
                └── ai/
                    └── mcp/
                        └── sample/
                            └── server/
```

### Key Components

- `McpServerProperties`: Configuration properties for the MCP server
- `TransportManager`: Manages the initialization and lifecycle of transport modes
- `StdioTransport`: Handles STDIO transport mode
- `OAuth2SecurityConfig`: OAuth2 security configuration

## License

This project is licensed under the MIT License - see the LICENSE file for details.