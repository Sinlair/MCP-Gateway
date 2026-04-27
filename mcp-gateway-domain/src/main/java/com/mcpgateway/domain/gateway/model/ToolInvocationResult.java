package com.mcpgateway.domain.gateway.model;

import java.util.Map;

public record ToolInvocationResult(
        String requestId,
        String sessionId,
        String clientId,
        String serverCode,
        String toolName,
        String toolIdentifier,
        String status,
        Map<String, Object> output
) {
}

