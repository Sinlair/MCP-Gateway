package com.mcpgateway.domain.upstream.repository;

import com.mcpgateway.domain.upstream.model.UpstreamServer;
import java.util.List;
import java.util.Optional;

public interface UpstreamServerRepository {

    UpstreamServer save(UpstreamServer upstreamServer);

    Optional<UpstreamServer> findByEnvironmentAndServerCode(String environment, String serverCode);

    List<UpstreamServer> findAllByEnvironment(String environment);
}

