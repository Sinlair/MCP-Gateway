package com.mcpgateway.domain.upstream.service;

import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.model.UpstreamRegistrationCommand;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import com.mcpgateway.domain.upstream.repository.UpstreamHealthProbe;
import com.mcpgateway.domain.upstream.repository.UpstreamServerRepository;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.time.Instant;
import java.util.List;

public class UpstreamRegistrationService {

    private final UpstreamServerRepository upstreamServerRepository;
    private final UpstreamHealthProbe upstreamHealthProbe;

    public UpstreamRegistrationService(
            UpstreamServerRepository upstreamServerRepository,
            UpstreamHealthProbe upstreamHealthProbe
    ) {
        this.upstreamServerRepository = upstreamServerRepository;
        this.upstreamHealthProbe = upstreamHealthProbe;
    }

    public UpstreamServer register(UpstreamRegistrationCommand command) {
        upstreamServerRepository.findByEnvironmentAndServerCode(command.environment(), command.serverCode())
                .ifPresent(existing -> {
                    throw new AppException(
                            ResponseCode.CONFLICT,
                            "serverCode already exists in environment: " + command.serverCode()
                    );
                });
        return upstreamServerRepository.save(UpstreamServer.register(command));
    }

    public List<UpstreamServer> listByEnvironment(String environment) {
        return upstreamServerRepository.findAllByEnvironment(environment);
    }

    public List<UpstreamServer> listEnabledByEnvironment(String environment) {
        return listByEnvironment(environment).stream()
                .filter(UpstreamServer::enabled)
                .toList();
    }

    public List<UpstreamServer> listRoutableByEnvironment(String environment) {
        return listByEnvironment(environment).stream()
                .filter(UpstreamServer::enabled)
                .filter(server -> server.healthStatus() == HealthStatus.UP)
                .toList();
    }

    public UpstreamServer refreshStatus(String environment, String serverCode) {
        UpstreamServer server = upstreamServerRepository.findByEnvironmentAndServerCode(environment, serverCode)
                .orElseThrow(() -> new AppException(
                        ResponseCode.NOT_FOUND,
                        "upstream server not found: " + serverCode
                ));
        HealthStatus status = upstreamHealthProbe.probe(server);
        UpstreamServer updated = server.withHealthStatus(status, Instant.now());
        return upstreamServerRepository.save(updated);
    }
}

