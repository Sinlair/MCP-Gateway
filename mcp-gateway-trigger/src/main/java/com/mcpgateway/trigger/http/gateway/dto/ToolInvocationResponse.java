package com.mcpgateway.trigger.http.gateway.dto;

import com.mcpgateway.domain.gateway.model.ToolInvocationResult;
import java.util.Map;

public record ToolInvocationResponse(
        String requestId,
        String sessionId,
        String callerId,
        String serverCode,
        String toolName,
        String toolIdentifier,
        String status,
        Map<String, Object> output
) {

    public static ToolInvocationResponse from(ToolInvocationResult result) {
        return new ToolInvocationResponse(
                result.requestId(),
                result.sessionId(),
                result.clientId(),
                result.serverCode(),
                result.toolName(),
                result.toolIdentifier(),
                result.status(),
                result.output()
        );
    }
}

