package com.mcpgateway.domain.tool.model;

public record ToolRegistrationCommand(
        String environment,
        String serverCode,
        String toolName,
        String description,
        String inputSchema,
        boolean enabled
) {
}

