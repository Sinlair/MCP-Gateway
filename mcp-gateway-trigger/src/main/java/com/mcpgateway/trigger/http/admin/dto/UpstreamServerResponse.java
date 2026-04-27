package com.mcpgateway.trigger.http.admin.dto;

import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.model.TransportType;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import java.time.Instant;

public record UpstreamServerResponse(
        String environment,
        String serverCode,
        String name,
        String baseUrl,
        TransportType transportType,
        String authMode,
        boolean enabled,
        int timeoutMs,
        HealthStatus healthStatus,
        Instant lastCheckedAt
) {

    public static UpstreamServerResponse from(UpstreamServer server) {
        return new UpstreamServerResponse(
                server.environment(),
                server.serverCode(),
                server.name(),
                server.baseUrl(),
                server.transportType(),
                server.authMode(),
                server.enabled(),
                server.timeoutMs(),
                server.healthStatus(),
                server.lastCheckedAt()
        );
    }
}

