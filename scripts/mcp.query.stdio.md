# MCP Query Server - STDIO Mode

## Overview

The MCP Query Server is a Spring Boot application that implements the Machine Conversation Protocol (MCP) server with STDIO transport mode. It provides a standardized interface for natural language to query API translation.

## Core Features (MVP)

| Feature                    | Description                                                | Value Proposition                                    | Implementation Approach                    |
|---------------------------|------------------------------------------------------------|------------------------------------------------------|-------------------------------------------|
| MCP Protocol Compliance    | Implements MCP server endpoints and message formats over stdio | Ensures any MCP client can connect without custom protocol work | Spring Boot + custom stdin/stdout handlers |
| NL-to-Query-API Translation | Converts natural language queries to structured API calls  | Enables intuitive querying without learning API syntax | Spring AI + custom query parser           |
| OAuth2 Authentication      | Secure API access with OAuth2 token management             | Enterprise-grade security and access control         | Spring Security + OAuth2 client           |
| Error Handling & Logging   | Comprehensive error handling and structured logging        | Reliable operation and easy troubleshooting          | Spring Boot + SLF4J                       |

## User Experience

### Key User Flows

1. Client runs `java -jar query-mcp-server.jar` and opens MCP channel (stdio).
2. Client sends natural language query through stdin.
3. Server processes query and returns structured response through stdout.
4. Client parses response and displays results.

### UI/UX Considerations

- Protocol exchanges are JSON over stdio—no UI in the server.
- Client responsible for user interface and experience.
- Structured error messages for client-side handling.
- Progress updates for long-running operations.

## Technical Architecture

### System Components

- MCP protocol handler (stdio)
- Query parser and translator
- OAuth2 authentication manager
- Error handling and logging system

### Data Models

- Query request/response objects
- Authentication tokens and credentials
- Error and status messages

### APIs

- Inbound: MCP endpoints over stdin/stdout
- Outbound: Query API integration
- Authentication: OAuth2 token management

### Infrastructure Requirements

- Java runtime environment
- Network access to query API
- OAuth2 credentials

## Development Roadmap

### Phase 1: Core Protocol
- [x] Basic MCP protocol implementation
- [x] STDIO transport layer
- [x] Error handling framework

### Phase 2: Query Translation
- [x] Natural language parsing
- [x] Query API integration
- [x] Response formatting

### Phase 3: Authentication
- [x] OAuth2 client implementation
- [x] Token management
- [x] Security best practices

### Phase 4: Production Readiness
- [x] Comprehensive logging
- [x] Performance optimization
- [x] Documentation

## Risks and Mitigations

| Risk                          | Impact | Likelihood | Mitigation Strategy                    |
|-------------------------------|--------|------------|----------------------------------------|
| Protocol compatibility issues | High   | Low        | Comprehensive testing with MCP clients |
| Query translation accuracy    | High   | Medium     | Regular validation and feedback loop   |
| Authentication failures       | High   | Low        | Robust error handling and retry logic  |
| Performance bottlenecks       | Medium | Medium     | Monitoring and optimization            |

## Appendix

### Configuration Reference

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

### Error Codes

| Code | Description                    | Resolution                          |
|------|--------------------------------|-------------------------------------|
| E001 | Protocol violation            | Check client implementation         |
| E002 | Authentication failure        | Verify credentials and permissions  |
| E003 | Query translation error       | Review query syntax and parameters  |
| E004 | API communication error       | Check network and API availability  |
| E005 | Internal server error         | Review server logs for details      |


