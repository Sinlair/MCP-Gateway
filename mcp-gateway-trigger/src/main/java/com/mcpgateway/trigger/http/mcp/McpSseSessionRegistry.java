package com.mcpgateway.trigger.http.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpgateway.domain.security.model.GatewayClient;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class McpSseSessionRegistry {

    private final ConcurrentMap<String, McpSseSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public McpSseSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpSseSession openSession(GatewayClient client, String environment) {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        McpSseSession session = new McpSseSession(sessionId, environment, client, emitter);
        sessions.put(sessionId, session);
        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> {
            sessions.remove(sessionId);
            emitter.complete();
        });
        emitter.onError(error -> sessions.remove(sessionId));
        return session;
    }

    public Optional<McpSseSession> find(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void sendEndpoint(McpSseSession session, String endpoint) throws IOException {
        session.emitter().send(SseEmitter.event().name("endpoint").data(endpoint));
    }

    public void sendMessage(McpSseSession session, Object payload) throws IOException {
        session.emitter().send(
                SseEmitter.event().name("message").data(objectMapper.writeValueAsString(payload))
        );
    }
}
