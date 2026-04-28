package com.mcpgateway.trigger.http.mcp;

import com.mcpgateway.domain.security.model.GatewayClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record McpSseSession(
        String sessionId,
        String environment,
        GatewayClient client,
        SseEmitter emitter
) {
}
