# Spring AI MCP Intacct Query STDIO Server

A Spring Boot starter project demonstrating how to build a Model Context Protocol (MCP) server that provides Sage Intacct query and model definition tools using the Intacct Core API. This project showcases the Spring AI MCP Server Boot Starter capabilities with STDIO transport implementation.

## ⚠️ IMPORTANT: Environment Migration Notice

**Sage Intacct has deprecated the legacy partner environment `partner.intacct.com` as of June 13, 2025.**

**Action Required:**
- Update your configuration to use the new partner environment URLs
- Contact your Sage Intacct administrator for your new partner environment URL
- Replace all instances of `partner.intacct.com` with your assigned new environment

**Example Migration:**
```bash
# OLD (deprecated)
-Dintacct.base.url=https://partner.intacct.com/ia/api/v1-beta2

# NEW (update with your assigned environment)
-Dintacct.base.url=https://your-new-partner-env.intacct.com/ia/api/v1-beta2
```

For more information, see the [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) reference documentation.

## Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Understanding of Spring Boot and Spring AI concepts
- Sage Intacct account with API access
- OAuth2 credentials for Intacct API
- (Optional) Claude Desktop for AI assistant integration

## About Spring AI MCP Server Boot Starter

The `spring-ai-mcp-server-spring-boot-starter` provides:
- Automatic configuration of MCP server components
- Support for both synchronous and asynchronous operation modes
- STDIO transport layer implementation
- Flexible tool registration through Spring beans
- Change notification capabilities

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── org/springframework/ai/mcp/sample/server/
│   │       ├── McpServerApplication.java    # Main application class with tool registration
│   │       ├── QueryService.java           # Intacct query service with MCP tools
│   │       ├── ModelService.java           # Intacct model definition service
│   │       └── AuthService.java            # OAuth2 authentication service
│   └── resources/
│       ├── application.properties          # Server and transport configuration
│       └── common.openapi.yaml            # OpenAPI specification reference
└── test/
    └── java/
        └── org/springframework/ai/mcp/sample/
            ├── client/
            │   └── ClientStdio.java         # Test client implementation
            └── server/
                └── ModelServiceTest.java    # Simple test for ModelService
```

## Building and Running

The server uses STDIO transport mode and is typically started automatically by the client. To build the server jar:

```bash
./mvnw clean install -DskipTests
```

To run standalone for testing:

```bash
java -Dspring.ai.mcp.server.transport=STDIO \
     -Dspring.main.web-application-type=none \
     -Dlogging.pattern.console= \
     -Dintacct.client-id=YOUR_CLIENT_ID \
     -Dintacct.client-secret=YOUR_CLIENT_SECRET \
     -Dintacct.username=YOUR_USERNAME \
     -Dintacct.password=YOUR_PASSWORD \
     -jar target/mcp-query-stdio-server-0.1.0.jar
```

## Tool Implementation

The project demonstrates how to implement and register MCP tools using Spring's dependency injection and auto-configuration:

```java
@Service
public class QueryService {
    @Tool(description = "Query data from a Sage Intacct object using filters")
    public List<Map<String, Object>> executeQuery(
        String object,           // Object type (e.g., "accounts-payable/vendor")
        List<String> fields,     // Fields to include
        List<Map<String, Map<String, Object>>> filters, // Filter conditions
        String filterExpression, // Logical operators for filters
        // ... other parameters
    ) {
        // Implementation
    }
}

@Service
public class ModelService {
    @Tool(description = "Get an object model definition")
    public ObjectModel getModelDefinition(
        String name,    // Resource name
        String type,    // Optional resource type
        // ... other parameters
    ) {
        // Implementation
    }

    @Tool(description = "List all available Intacct resource model summaries")
    public List<ResourceSummary> listAvailableModels() {
        // Implementation
    }
}

@SpringBootApplication
public class McpServerApplication {
    @Bean
    public ToolCallbackProvider intacctQueryTools(QueryService queryService) {
        return MethodToolCallbackProvider.builder().toolObjects(queryService).build();
    }

    @Bean
    public ToolCallbackProvider intacctModelTools(ModelService modelService) {
        return MethodToolCallbackProvider.builder().toolObjects(modelService).build();
    }
}
```

## Available Tools

### 1. Query Tool (`executeQuery`)

Query data from Sage Intacct objects with flexible filtering, field selection, and pagination.

**Parameters:**
- `object` (required): Object type to query (e.g., `"accounts-payable/vendor"`)
- `fields` (optional): List of fields to include (e.g., `["id", "name", "status"]`)
- `filters` (optional): Array of filter conditions using operators like `$eq`, `$gt`, `$contains`, etc.
- `filterExpression` (optional): Logical combination of filters (e.g., `"1 and 2"`)
- `orderBy` (optional): Sort order (e.g., `[{"id": "asc"}]`)
- `start` (optional): Starting record for pagination
- `size` (optional): Number of records to return

**Example usage:**
```json
{
  "object": "accounts-payable/vendor",
  "fields": ["id", "name", "status"],
  "filters": [{"$eq": {"status": "active"}}],
  "orderBy": [{"id": "asc"}],
  "size": 10
}
```

**Supported Filter Operators:**
- `$eq`: Equal to
- `$ne`: Not equal to
- `$lt`, `$lte`: Less than (or equal)
- `$gt`, `$gte`: Greater than (or equal)
- `$in`, `$notIn`: In/not in list of values
- `$between`, `$notBetween`: Between two values
- `$contains`, `$notContains`: Contains substring
- `$startsWith`, `$endsWith`: String pattern matching

### 2. Model Definition Tool (`getModelDefinition`)

Retrieve detailed model definitions for Intacct objects, including field types, relationships, and constraints.

**Parameters:**
- `name` (required): Resource name (e.g., `"accounts-payable/vendor"`)
- `type` (optional): Resource type filter (`"object"`, `"service"`)
- `version` (optional): API version (`"v1"`, `"ALL"`)
- `schema` (optional): Include full schema (`"true"`/`"false"`)
- `tags` (optional): Schema formatting (`"true"`/`"false"`)

### 3. List Models Tool (`listAvailableModels`)

Get a summary of all available Intacct resource models.

**Returns:** List of resource summaries with API object names and types.

## Authentication Configuration

The server requires OAuth2 credentials for Intacct API access. Configure through:

### System Properties:
```bash
# Core authentication
-Dintacct.client-id=your_client_id
-Dintacct.client-secret=your_client_secret
-Dintacct.username=your_username
-Dintacct.password=your_password

# API endpoints (all configurable) - UPDATE TO YOUR NEW ENVIRONMENT
-Dintacct.base.url=https://your-new-partner-env.intacct.com/ia/api/v1-beta2
-Dintacct.token.endpoint=https://your-new-partner-env.intacct.com/ia/api/v1-beta2/oauth2/token

# Alternative environment variables
-DOAUTH2_CLIENT_ID=your_client_id
-DOAUTH2_CLIENT_SECRET=your_client_secret
-DOAUTH2_USERNAME=your_username
-DOAUTH2_PASSWORD=your_password
-DINTACCT_BASE_URL=https://partner.intacct.com/ia/api/v1-beta2
-DOAUTH2_AUTH_URI=https://partner.intacct.com/ia/api/v1-beta2/oauth2/authorize
-DOAUTH2_TOKEN_URI=https://partner.intacct.com/ia/api/v1-beta2/oauth2/token
-DOAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/mcp-client
```

### Application Properties:
```properties
# Core authentication
intacct.client-id=your_client_id
intacct.client-secret=your_client_secret
intacct.username=your_username
intacct.password=your_password

# API endpoints (all configurable)
intacct.base.url=https://partner.intacct.com/ia/api/v1-beta2
intacct.token.endpoint=https://partner.intacct.com/ia/api/v1-beta2/oauth2/token

# MCP Server configuration (via application.yml)
mcp.server.auth.base-url=https://partner.intacct.com/ia/api/v1-beta2
mcp.server.auth.oauth2.client-id=your_client_id
mcp.server.auth.oauth2.client-secret=your_client_secret
mcp.server.auth.oauth2.authorization-uri=https://partner.intacct.com/ia/api/v1-beta2/oauth2/authorize
mcp.server.auth.oauth2.token-uri=https://partner.intacct.com/ia/api/v1-beta2/oauth2/token
mcp.server.auth.oauth2.redirect-uri=http://localhost:8080/login/oauth2/code/mcp-client
```

### Environment Variables:
Set corresponding environment variables for containerized deployments.

## Client Integration

### Java Client Example

```java
// Create server parameters
ServerParameters stdioParams = ServerParameters.builder("java")
    .args("-Dspring.ai.mcp.server.transport=STDIO",
          "-Dspring.main.web-application-type=none",
          "-Dlogging.pattern.console=",
          "-Dintacct.client-id=" + clientId,
          "-Dintacct.client-secret=" + clientSecret,
          "-Dintacct.username=" + username,
          "-Dintacct.password=" + password,
          "-jar",
          "target/mcp-query-stdio-server-0.1.0.jar")
    .build();

// Initialize transport and client
var transport = new StdioClientTransport(stdioParams);
var client = McpClient.sync(transport).build();

// Query active vendors
CallToolResult result = client.callTool(
    new CallToolRequest("executeQuery",
        Map.of(
            "object", "accounts-payable/vendor",
            "fields", List.of("id", "name", "status"),
            "filters", List.of(Map.of("$eq", Map.of("status", "active"))),
            "size", 5
        )
    )
);
```

### Claude Desktop Integration

Add to Claude Desktop configuration:

```json
{
  "mcpServers": {
    "intacct-query": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.transport=STDIO",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-Dintacct.client-id=YOUR_CLIENT_ID",
        "-Dintacct.client-secret=YOUR_CLIENT_SECRET",
        "-Dintacct.username=YOUR_USERNAME",
        "-Dintacct.password=YOUR_PASSWORD",
        "-Dintacct.base.url=https://partner.intacct.com/ia/api/v1-beta2",
        "-Dintacct.token.endpoint=https://partner.intacct.com/ia/api/v1-beta2/oauth2/token",
        "-jar",
        "/absolute/path/to/mcp-query-stdio-server-0.1.0.jar"
      ]
    }
  }
}
```

## Configuration

### Required STDIO Configuration
```properties
# Disable web application and console output for STDIO
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.pattern.console=

# MCP Server Configuration
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.name=query-server
spring.ai.mcp.server.version=0.1.0
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.transport=STDIO
```

### Authentication and Caching
```properties
# OAuth2 Token Caching (Caffeine)
caffeine.auth.cache.max-size=10
caffeine.auth.cache.ttl-seconds=3300

# File Logging (console disabled for STDIO)
logging.file.name=./mcp-server-query.log
logging.level.root=OFF
```

## Testing

### Unit Testing
```bash
# Run all tests
./mvnw test

# Test ModelService standalone
./mvnw compile exec:java -Dexec.mainClass="org.springframework.ai.mcp.sample.server.ModelService"

# Test QueryService standalone  
./mvnw compile exec:java -Dexec.mainClass="org.springframework.ai.mcp.sample.server.QueryService"
```

### Integration Testing
Use the provided `ClientStdio.java` for full client-server integration testing.

## API Examples

### Query Vendors by Status
```json
{
  "object": "accounts-payable/vendor",
  "fields": ["id", "name", "status", "billingType"],
  "filters": [{"$eq": {"status": "active"}}],
  "orderBy": [{"name": "asc"}],
  "size": 20
}
```

### Query with Multiple Filters
```json
{
  "object": "accounts-payable/vendor", 
  "fields": ["id", "name", "totalDue"],
  "filters": [
    {"$eq": {"status": "active"}},
    {"$gt": {"totalDue": 1000}},
    {"$eq": {"country": "USA"}}
  ],
  "filterExpression": "(1 or 2) and 3",
  "size": 50
}
```

### Get Department Model
```json
{
  "name": "company-config/department",
  "schema": "true"
}
```

## Security Notes

- Store API credentials securely (environment variables, secrets management)
- Access tokens are cached with automatic refresh
- Use HTTPS endpoints in production
- Implement proper error handling for authentication failures

## Additional Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [Model Context Protocol Specification](https://modelcontextprotocol.github.io/specification/)
- [Sage Intacct API Documentation](https://developer.intacct.com/)