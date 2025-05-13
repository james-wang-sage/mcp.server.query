# Overview  
**Project Name**  
Query MCP Server  

**Description**  
Query MCP Server is a Model Context Protocol (MCP)–compliant server that sits between any MCP client (e.g. Sage Copilot chatbot) and the Sage Intacct Query REST API. It enables LLM-driven applications to query Intacct product data via natural-language prompts.  

**Problem & Value**  
-  Problem: Integrating LLMs with Sage Intacct requires custom coding for authentication, payload construction, error handling, etc.  
-  Value: Provides a plug-and-play MCP server that handles protocol compliance, NL-to-API translation, OAuth2 auth, and Intacct calls so clients can focus on UI/UX and domain logic.  

# Core Features (MVP)  
| Feature                          | What                                                                                  | Why                                                                    | How                                                                           |
|----------------------------------|---------------------------------------------------------------------------------------|------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| MCP Protocol Compliance          | Implements MCP server endpoints and message formats over stdio and SSE               | Ensures any MCP client can connect without custom protocol work        | Spring Boot + custom stdin/stdout & SSE handlers                              |
| NL-to-Query-API Translation       | Transforms user prompts into Sage Intacct Query API payloads                          | Allows non-technical users to fetch Intacct data with natural language | Spring AI prompts → JSON conforming to Query API schema                       |
| OAuth2 Authentication            | Accepts user & company credentials, obtains and refreshes access tokens               | Secures calls to Intacct and isolates each user/company context         | Spring Security OAuth2 client, token store, request interceptors              |
| Intacct REST Client              | Sends authenticated REST requests to `partner.intacct.com/query` and parses responses | Centralizes API integration logic and error handling                   | Spring WebClient with custom codecs                                           |
| Error Handling & Logging         | Catches translation, auth, or API errors and returns structured MCP error messages   | Ensures clients receive clear diagnostics and logs for troubleshooting | Global exception handlers, SLF4J + Logback, standardized JSON error schema    |
| Packaging & Deployment           | Packaged as a standalone “fat” JAR for Java CLI                                      | Simplifies distribution and execution                                  | Spring Boot Maven plugin                                                      |

# User Experience  
**User Personas**  
-  Sage Copilot Chatbot: LLM application that issues natural-language queries to Intacct.  
-  MCP Client Developers: Engineers embedding MCP into their tools to add Intacct data access.  

**Key User Flows**  
1. Client runs `java -jar query-mcp-server.jar` and opens MCP channel (stdio or SSE).  
2. Copilot sends an MCP “invoke” request with a natural-language query.  
3. Server authenticates via OAuth2, translates the prompt, calls Intacct, and streams results back over MCP.  
4. Client renders results or handles errors.  

**UI/UX Considerations**  
-  Protocol exchanges are JSON over stdio or SSE—no UI in the server.  
-  Clients display streamed JSON chunks or final payload as appropriate.  

# Technical Architecture  
**System Components**  
-  Spring Boot application  
-  NL-to-API translation service (Spring AI)  
-  OAuth2 client & token manager  
-  REST client for Intacct  
-  MCP transport handlers (stdio & SSE)  

**Data Models**  
-  `McpRequest` / `McpResponse` (standard MCP envelope)  
-  `IntacctQueryRequest` (fields: object, filters, fields, etc.)  
-  `IntacctQueryResponse` (rows, metadata)  
-  `OAuth2Token` (access token, refresh token, expiry)  

**APIs & Integrations**  
-  Inbound: MCP endpoints over stdin/stdout and SSE  
-  Outbound: HTTPS POST to `https://partner.intacct.com/query` with OAuth2 Bearer token  

**Infrastructure Requirements**  
-  Java 17+ runtime  
-  Standalone “fat” JAR (no external containers)  
-  Optional: Redirect stdout/stderr to file or pipeline for logging  

# Development Roadmap  
**Phase 1 (MVP)**  
-  MCP protocol handler (stdio & SSE)  
-  Spring Boot skeleton & CLI launcher  
-  OAuth2 authentication flows  
-  Basic NL-to-Payload translation stub  
-  REST client integration & response parsing  
-  Error handling & logging  
-  Build & package as fat JAR  

**Phase 2 (Enhancements)**  
-  Improve translation prompts & templates  
-  Token caching & auto-refresh optimization  
-  Retry logic for transient API failures  
-  Metrics & health-check endpoints  

**Phase 3 (Extensions)**  
-  Support additional Intacct APIs (create, update)  
-  Multi-model context handling  
-  Advanced analytics / caching layer  

# Logical Dependency Chain  
1. Spring Boot CLI application setup  
2. OAuth2 module & token management  
3. REST client configuration  
4. MCP transport handlers  
5. NL-to-Query translation integration  
6. Error handling & logging  
7. Build scripts & packaging  

# Risks and Mitigations  
| Risk                                    | Mitigation                                                       |
|-----------------------------------------|------------------------------------------------------------------|
| Accurate NL-to-API translation          | Start with simple templates; gather feedback; iterate            |
| Multi-tenant token handling complexity  | Leverage Spring Security and in-memory token store               |
| MVP scope creep                         | Strictly limit Phase 1 features; defer enhancements              |

# Appendix  
-  Intacct Query API docs: https://developer.intacct.com/api/query/  
-  MCP Protocol spec (internal)  
-  Spring AI reference guide  


