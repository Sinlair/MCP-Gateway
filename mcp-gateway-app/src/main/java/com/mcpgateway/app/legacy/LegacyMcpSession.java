package com.mcpgateway.app.legacy;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record LegacyMcpSession(
        String sessionId,
        String gatewayId,
        String apiKey,
        SseEmitter emitter
) {
}
