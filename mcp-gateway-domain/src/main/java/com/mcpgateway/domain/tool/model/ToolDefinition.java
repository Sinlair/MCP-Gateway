package com.mcpgateway.domain.tool.model;

import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;

public record ToolDefinition(
        String environment,
        String serverCode,
        String toolName,
        String description,
        String inputSchema,
        boolean enabled
) {

    public ToolDefinition {
        if (isBlank(environment) || isBlank(serverCode) || isBlank(toolName)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "tool definition is incomplete");
        }
    }

    public static ToolDefinition register(ToolRegistrationCommand command) {
        return new ToolDefinition(
                command.environment(),
                command.serverCode(),
                command.toolName(),
                command.description(),
                command.inputSchema(),
                command.enabled()
        );
    }

    public String toolIdentifier() {
        return serverCode + ":" + toolName;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

