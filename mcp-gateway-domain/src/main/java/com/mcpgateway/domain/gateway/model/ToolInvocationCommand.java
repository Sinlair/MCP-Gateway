package com.mcpgateway.domain.gateway.model;

import java.util.Map;

public record ToolInvocationCommand(
        String environment,
        String toolIdentifier,
        Map<String, Object> arguments
) {
}

