package com.mcpgateway.domain.tool.service;

import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.tool.model.ToolRegistrationCommand;
import com.mcpgateway.domain.tool.repository.ToolDefinitionRepository;
import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.repository.UpstreamServerRepository;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.util.List;
import java.util.Set;

public class ToolCatalogService {

    private final ToolDefinitionRepository toolDefinitionRepository;
    private final UpstreamServerRepository upstreamServerRepository;

    public ToolCatalogService(
            ToolDefinitionRepository toolDefinitionRepository,
            UpstreamServerRepository upstreamServerRepository
    ) {
        this.toolDefinitionRepository = toolDefinitionRepository;
        this.upstreamServerRepository = upstreamServerRepository;
    }

    public ToolDefinition register(ToolRegistrationCommand command) {
        upstreamServerRepository.findByEnvironmentAndServerCode(command.environment(), command.serverCode())
                .orElseThrow(() -> new AppException(
                        ResponseCode.NOT_FOUND,
                        "upstream server not found for tool registration: " + command.serverCode()
                ));
        return toolDefinitionRepository.save(ToolDefinition.register(command));
    }

    public List<ToolDefinition> listByEnvironment(String environment) {
        return toolDefinitionRepository.findAllByEnvironment(environment);
    }

    public List<ToolDefinition> listDiscoverableByEnvironment(String environment) {
        Set<String> routableServers = upstreamServerRepository.findAllByEnvironment(environment).stream()
                .filter(server -> server.enabled() && server.healthStatus() == HealthStatus.UP)
                .map(server -> server.serverCode())
                .collect(java.util.stream.Collectors.toSet());
        return toolDefinitionRepository.findAllByEnvironment(environment).stream()
                .filter(ToolDefinition::enabled)
                .filter(tool -> routableServers.contains(tool.serverCode()))
                .toList();
    }
}

