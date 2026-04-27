package com.mcpgateway.trigger.http.admin.tool.dto;

import com.mcpgateway.domain.tool.model.ToolDefinition;

public record ToolDefinitionResponse(
        String environment,
        String serverCode,
        String toolName,
        String toolIdentifier,
        String description,
        String inputSchema,
        boolean enabled
) {

    public static ToolDefinitionResponse from(ToolDefinition toolDefinition) {
        return new ToolDefinitionResponse(
                toolDefinition.environment(),
                toolDefinition.serverCode(),
                toolDefinition.toolName(),
                toolDefinition.toolIdentifier(),
                toolDefinition.description(),
                toolDefinition.inputSchema(),
                toolDefinition.enabled()
        );
    }
}

