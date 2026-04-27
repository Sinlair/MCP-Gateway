package com.mcpgateway.domain.upstream.repository;

import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.model.UpstreamServer;

public interface UpstreamHealthProbe {

    HealthStatus probe(UpstreamServer server);
}

