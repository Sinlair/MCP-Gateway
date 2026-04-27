package com.mcpgateway.domain.gateway.model;

import java.util.Map;

public record ToolExecutionResult(
        String status,
        Map<String, Object> output
) {
}

