# Intacct DS MCP Query Server

A Spring Boot application that implements the Model Context Protocol (MCP) server for Sage Intacct Query API integration. This server acts as a bridge between MCP clients (like AI assistants) and the Sage Intacct Query REST API, enabling natural language queries against Intacct data.

## Features

- **MCP Protocol Compliance**: Full implementation of MCP server specification
- **Sage Intacct Integration**: Direct integration with Intacct Core Query and Model APIs
- **OAuth2 Authentication**: Secure authentication with token caching using Caffeine
- **STDIO Transport**: Process-based communication via stdin/stdout
- **Spring AI Tools**: Annotated tools for AI model integration
- **Comprehensive Error Handling**: Robust error handling and logging

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Sage Intacct API credentials (OAuth2)

## Configuration

The server can be configured through `application.yml` or environment variables:

### Environment Variables

```bash
export OAUTH2_CLIENT_ID="your-client-id"
export OAUTH2_CLIENT_SECRET="your-client-secret"
export OAUTH2_USERNAME="your-username"
export OAUTH2_PASSWORD="your-password"
export INTACCT_BASE_URL="https://api-partner-main.intacct.com/ia/api/v1-beta2"
```

### Configuration

```yaml
mcp:
  server:
    name: query-server
    version: 0.1.0
    type: SYNC
    stdio:
      enabled: true
      disable-console-logging: true
      disable-banner: true
    auth:
      mode: OAUTH2
      oauth2:
        client-id: ${OAUTH2_CLIENT_ID}
        client-secret: ${OAUTH2_CLIENT_SECRET}
        username: ${OAUTH2_USERNAME}
        password: ${OAUTH2_PASSWORD}
        base-url: ${INTACCT_BASE_URL}
```

## Building

```bash
# Clean build
mvn clean package

# Build with tests
mvn clean package -DskipTests=false
```

## Running

```bash
# Set environment variables first
export OAUTH2_CLIENT_ID="your-client-id"
export OAUTH2_CLIENT_SECRET="your-client-secret"
export OAUTH2_USERNAME="your-username"
export OAUTH2_PASSWORD="your-password"

# Run the server
java -jar target/mcp-query-stdio-server-0.1.0.jar
```

## Development

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── intacct/
│   │           └── ds/
│   │               └── mcp/
│   │                   └── server/
│   │                       └── query/
│   │                           ├── McpServerApplication.java
│   │                           ├── config/
│   │                           │   ├── McpServerConfiguration.java
│   │                           │   └── McpServerProperties.java
│   │                           ├── security/
│   │                           │   ├── AuthenticationContext.java
│   │                           │   ├── OAuth2SecurityConfig.java
│   │                           │   └── SecurityContext.java
│   │                           ├── service/
│   │                           │   ├── AuthService.java
│   │                           │   ├── ModelService.java
│   │                           │   └── QueryService.java
│   │                           └── transport/
│   │                               ├── StdioTransport.java
│   │                               └── TransportManager.java
│   └── resources/
│       ├── application.yml
│       ├── application.properties
│       └── common.openapi.yaml
└── test/
    ├── java/
    │   └── com/intacct/ds/mcp/server/query/
    │       ├── McpFunctionalityTest.java
    │       ├── ObjectPrefixFixTest.java
    │       └── config/
    │           └── McpTestConfiguration.java
    └── resources/
        ├── application-test.yml
        └── application-dev.yml
```

### Core Services

#### AuthService
- **Purpose**: OAuth2 authentication and token management for Intacct API
- **Features**:
  - Token caching with Caffeine
  - Automatic token refresh
  - Thread-safe token operations
  - Configurable cache settings

#### QueryService
- **Purpose**: Execute queries against Sage Intacct objects
- **Tools**: `@Tool executeQuery` - Query data with filters, pagination, and sorting
- **Features**:
  - Support for complex filter expressions
  - Field selection and ordering
  - Pagination support
  - Automatic "objects/" prefix handling

#### ModelService
- **Purpose**: Retrieve model definitions for Intacct resources
- **Tools**:
  - `@Tool getModelDefinition` - Get detailed model schema
  - `@Tool listAvailableModels` - List all available models
- **Features**:
  - Resource schema discovery
  - Field definitions and relationships
  - API version support

### Transport Layer

#### TransportManager
- **Purpose**: Manages initialization and lifecycle of transport mode
- **Features**: STDIO transport management and health checking

#### StdioTransport
- **Purpose**: Handles STDIO transport mode for MCP communication
- **Features**: Process-based communication via stdin/stdout

## Available Tools

The server exposes the following tools for AI model integration:

### Query Tools

#### `executeQuery`
Execute queries against Sage Intacct objects with advanced filtering and pagination.

**Parameters:**
- `object` (required): Object type (e.g., "accounts-payable/vendor")
- `fields` (optional): List of fields to include
- `filters` (optional): Filter conditions using operators like $eq, $gt, $in, etc.
- `filterExpression` (optional): Logical combination of filters
- `orderBy` (optional): Sort order specification
- `start` (optional): Starting record for pagination
- `size` (optional): Number of records to return

**Example:**
```json
{
  "object": "accounts-payable/vendor",
  "fields": ["id", "name", "status", "totalDue"],
  "filters": [
    {"$eq": {"status": "active"}},
    {"$gt": {"totalDue": 1000}}
  ],
  "filterExpression": "1 and 2",
  "orderBy": [{"name": "asc"}],
  "size": 10
}
```

### Model Tools

#### `getModelDefinition`
Retrieve detailed model schema for Intacct resources.

**Parameters:**
- `name` (required): Resource name (e.g., "accounts-payable/vendor")
- `type` (optional): Resource type filter
- `version` (optional): API version
- `schema` (optional): Include full schema details
- `tags` (optional): Schema formatting options

#### `listAvailableModels`
List all available Intacct resource models.

**Returns:** List of available resources with their types and descriptions.

## Usage Examples

### Standalone Testing

```bash
# Test ModelService
mvn compile exec:java -Dexec.mainClass="com.intacct.ds.mcp.server.query.service.ModelService" \
  -Dexec.classpathScope="test" -Dexec.args="accounts-payable/vendor"

# Test QueryService
mvn compile exec:java -Dexec.mainClass="com.intacct.ds.mcp.server.query.service.QueryService" \
  -Dexec.classpathScope="test"
```
