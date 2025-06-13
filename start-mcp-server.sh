#!/bin/bash

# Set system properties for MCP Server
export JAVA_OPTS="-Dspring.ai.mcp.server.stdio=true \
-Dspring.main.web-application-type=none \
-Dlogging.pattern.console= \
-Dlogging.file.name=/Users/jameswang/Library/Logs/Claude/mcp-query-stdio-server.log"

# Run the application
java $JAVA_OPTS -jar target/mcp-query-stdio-server-0.1.0.jar 