# Spring AI MCP Intacct Query Server - Project Summary

## Project Overview

This is a **Spring Boot application** that implements a **Model Context Protocol (MCP) server** for querying **Sage Intacct** accounting data. The project demonstrates the Spring AI MCP Server Boot Starter capabilities with support for multiple transport modes (STDIO and SSE) and OAuth2 authentication.

## Core Architecture

### Technology Stack
- **Java 17+** with **Spring Boot 3.3.6**
- **Spring AI 1.0.0** with MCP Server Boot Starter
- **Spring Security** with OAuth2 client support
- **Spring WebFlux** for reactive SSE support
- **Maven** for dependency management
- **Caffeine** for token caching
- **Jackson** for JSON processing

### Key Components

1. **McpServerApplication.java** - Main application class with profile-based startup logic
2. **QueryService.java** - Core service providing Intacct data querying capabilities via MCP tools
3. **ModelService.java** - Service for retrieving Intacct object model definitions
4. **AuthService.java** - OAuth2 authentication service for Intacct API access
5. **Transport Layer** - Supports both STDIO and SSE transport modes
6. **Security Configuration** - OAuth2 integration for web-based access

## Main Features

### 1. Intacct Data Querying (`QueryService`)
- **Primary Tool**: `executeQuery` - Query Intacct objects with advanced filtering
- **Supported Operations**:
  - Field selection (specify which fields to return)
  - Complex filtering with 15+ operators (`$eq`, `$gt`, `$contains`, `$between`, etc.)
  - Filter expressions with logical operators (`and`, `or`, parentheses)
  - Sorting and pagination
  - Date macro support (`today`, `currentMonth`, etc.)
- **Example Objects**: `accounts-payable/vendor`, `company-config/department`

### 2. Model Definition Retrieval (`ModelService`)
- **Tools**:
  - `getModelDefinition` - Get detailed schema for specific Intacct objects
  - `listAvailableModels` - List all available Intacct resource models
- **Returns**: Field definitions, relationships, data types, constraints

### 3. Multi-Transport Support
- **STDIO Mode** (Default): Process-based communication for AI clients like Claude Desktop
- **SSE Mode**: HTTP-based Server-Sent Events with REST endpoints
- **Dual Mode**: Both transports active simultaneously

### 4. Authentication & Security
- **OAuth2 Integration**: Full authorization code grant flow for Intacct API
- **Token Caching**: Automatic token refresh with Caffeine cache
- **CORS Support**: Configurable for web clients
- **Session Management**: Timeout handling and cleanup

## Project Structure

```
src/
├── main/java/org/springframework/ai/mcp/sample/server/
│   ├── McpServerApplication.java      # Main app with tool registration
│   ├── QueryService.java             # Intacct query service (21KB)
│   ├── ModelService.java             # Model definition service (15KB)
│   ├── AuthService.java              # OAuth2 authentication (8.7KB)
│   ├── config/                       # Configuration classes
│   ├── security/                     # Security components
│   └── transport/                    # Transport implementations
├── test/
│   ├── client/                       # Test client implementations
│   └── server/                       # Unit and integration tests
└── resources/
    ├── application.properties        # Main configuration
    └── common.openapi.yaml          # API specification
```

## Configuration Profiles

1. **`stdio` (Default)**: STDIO mode only, no web server
2. **`sse`**: SSE mode with web server and OAuth2
3. **`dual`**: Both STDIO and SSE modes active
4. **`dev`**: Development mode with SSE but no authentication

## API Examples

### Query Active Vendors
```json
{
  "object": "accounts-payable/vendor",
  "fields": ["id", "name", "status"],
  "filters": [{"$eq": {"status": "active"}}],
  "orderBy": [{"name": "asc"}],
  "size": 10
}
```

### Complex Filter Query
```json
{
  "object": "accounts-payable/vendor",
  "fields": ["id", "name", "totalDue"],
  "filters": [
    {"$eq": {"status": "active"}},
    {"$gt": {"totalDue": 1000}},
    {"$eq": {"country": "USA"}}
  ],
  "filterExpression": "(1 or 2) and 3"
}
```

## Deployment & Usage

### STDIO Mode (AI Clients)
```bash
java -Dspring.ai.mcp.server.transport=STDIO \
     -Dspring.main.web-application-type=none \
     -Dintacct.client-id=CLIENT_ID \
     -jar mcp-query-stdio-server-0.1.0.jar
```

### SSE Mode (Web Applications)
```bash
export OAUTH2_CLIENT_ID=your-client-id
java -jar mcp-query-stdio-server-0.1.0.jar --spring.profiles.active=sse
```

### Claude Desktop Integration
```json
{
  "mcpServers": {
    "intacct-query": {
      "command": "java",
      "args": ["-jar", "/path/to/mcp-query-stdio-server-0.1.0.jar"]
    }
  }
}
```

## Current State & Quality

### Strengths
- ✅ Well-structured Spring Boot application
- ✅ Comprehensive MCP tool implementation
- ✅ Multiple transport modes (STDIO/SSE)
- ✅ OAuth2 authentication integration
- ✅ Detailed documentation and examples
- ✅ Flexible query capabilities with 15+ filter operators
- ✅ Proper error handling and logging

### Areas for Improvement
- ⚠️ **Security**: Hardcoded credentials in `AuthService` (development only)
- ⚠️ **Testing**: Limited test coverage (only 2 test files)
- ⚠️ **Configuration**: Some hardcoded URLs and settings
- ⚠️ **Error Handling**: Could be more robust for production use

### Test Coverage
- `ModelServiceTest.java` - Basic unit test
- `McpServerIntegrationTest.java` - Integration test
- **Missing**: QueryService tests, authentication tests, transport tests

## Key Dependencies

```xml
<dependencies>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
    
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
    
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependencies>
```

## Use Cases

1. **AI Assistant Integration**: Connect Claude Desktop or other AI tools to Intacct data
2. **Business Intelligence**: Query accounting data with natural language
3. **Reporting & Analytics**: Flexible data extraction with complex filtering
4. **API Integration**: RESTful access to Intacct via SSE mode
5. **Development & Testing**: Dual-mode support for simultaneous access patterns

## Next Steps for Development

1. **Security Hardening**: Remove hardcoded credentials, implement proper secret management
2. **Test Coverage**: Add comprehensive unit and integration tests
3. **Production Configuration**: Externalize configuration, add health checks
4. **Documentation**: API documentation, deployment guides
5. **Monitoring**: Add metrics, logging improvements
6. **Error Handling**: Enhanced error responses and recovery mechanisms

This project demonstrates a sophisticated implementation of the Spring AI MCP framework for enterprise accounting system integration, with production-ready architecture and comprehensive querying capabilities.