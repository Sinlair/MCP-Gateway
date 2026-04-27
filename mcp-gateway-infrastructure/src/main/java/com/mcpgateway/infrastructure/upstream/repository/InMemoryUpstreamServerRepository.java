package com.mcpgateway.infrastructure.upstream.repository;

import com.mcpgateway.domain.upstream.model.UpstreamServer;
import com.mcpgateway.domain.upstream.repository.UpstreamServerRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUpstreamServerRepository implements UpstreamServerRepository {

    private final ConcurrentMap<String, UpstreamServer> storage = new ConcurrentHashMap<>();

    @Override
    public UpstreamServer save(UpstreamServer upstreamServer) {
        storage.put(key(upstreamServer.environment(), upstreamServer.serverCode()), upstreamServer);
        return upstreamServer;
    }

    @Override
    public Optional<UpstreamServer> findByEnvironmentAndServerCode(String environment, String serverCode) {
        return Optional.ofNullable(storage.get(key(environment, serverCode)));
    }

    @Override
    public List<UpstreamServer> findAllByEnvironment(String environment) {
        return storage.values().stream()
                .filter(server -> server.environment().equals(environment))
                .sorted(Comparator.comparing(UpstreamServer::serverCode))
                .toList();
    }

    private String key(String environment, String serverCode) {
        return environment + ":" + serverCode;
    }
}

