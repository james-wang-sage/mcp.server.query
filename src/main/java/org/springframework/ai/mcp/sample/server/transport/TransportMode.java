package org.springframework.ai.mcp.sample.server.transport;

/**
 * Enumeration of supported MCP transport modes.
 */
public enum TransportMode {
    /**
     * Standard Input/Output transport mode.
     * Used for direct process communication where the MCP server
     * communicates via stdin/stdout streams.
     */
    STDIO
}