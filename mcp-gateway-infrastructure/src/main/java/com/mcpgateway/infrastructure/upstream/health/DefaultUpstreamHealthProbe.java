package com.mcpgateway.infrastructure.upstream.health;

import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import com.mcpgateway.domain.upstream.repository.UpstreamHealthProbe;
import org.springframework.stereotype.Component;

@Component
public class DefaultUpstreamHealthProbe implements UpstreamHealthProbe {

    @Override
    public HealthStatus probe(UpstreamServer server) {
        if (!server.enabled()) {
            return HealthStatus.DOWN;
        }
        return server.baseUrl().startsWith("http://") || server.baseUrl().startsWith("https://")
                ? HealthStatus.UP
                : HealthStatus.DOWN;
    }
}

