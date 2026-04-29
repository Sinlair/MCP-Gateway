package com.mcpgateway.app.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class LegacyMcpSessionRegistry {

    private final ConcurrentMap<String, LegacyMcpSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public LegacyMcpSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LegacyMcpSession create(String gatewayId, String apiKey) {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        LegacyMcpSession session = new LegacyMcpSession(sessionId, gatewayId, apiKey, emitter);
        sessions.put(sessionId, session);
        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> {
            sessions.remove(sessionId);
            emitter.complete();
        });
        emitter.onError(error -> sessions.remove(sessionId));
        return session;
    }

    public Optional<LegacyMcpSession> find(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void sendEndpoint(LegacyMcpSession session, String endpoint) throws IOException {
        session.emitter().send(SseEmitter.event().name("endpoint").data(endpoint));
    }

    public void sendMessage(LegacyMcpSession session, Object payload) throws IOException {
        session.emitter().send(
                SseEmitter.event().name("message").data(objectMapper.writeValueAsString(payload))
        );
    }
}
