package com.mcpgateway.types.context;

public record GatewayRequestContext(
        String requestId,
        String sessionId,
        String clientId
) {
}

