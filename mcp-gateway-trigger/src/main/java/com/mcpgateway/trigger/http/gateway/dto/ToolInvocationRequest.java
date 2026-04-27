package com.mcpgateway.trigger.http.gateway.dto;

import com.mcpgateway.domain.gateway.model.ToolInvocationCommand;
import java.util.Map;

public record ToolInvocationRequest(
        String environment,
        String toolIdentifier,
        Map<String, Object> arguments
) {

    public ToolInvocationCommand toCommand() {
        return new ToolInvocationCommand(environment, toolIdentifier, arguments);
    }
}

