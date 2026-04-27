package com.mcpgateway.trigger.http.admin.tool.dto;

import com.mcpgateway.domain.tool.model.ToolRegistrationCommand;

public record ToolRegistrationRequest(
        String environment,
        String serverCode,
        String toolName,
        String description,
        String inputSchema,
        boolean enabled
) {

    public ToolRegistrationCommand toCommand() {
        return new ToolRegistrationCommand(environment, serverCode, toolName, description, inputSchema, enabled);
    }
}

