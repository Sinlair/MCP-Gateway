package com.mcpgateway.domain.tool.service;

import com.mcpgateway.domain.accesscontrol.service.ToolAccessPolicyService;
import com.mcpgateway.domain.gateway.model.ToolExecutionResult;
import com.mcpgateway.domain.gateway.model.ToolInvocationCommand;
import com.mcpgateway.domain.gateway.model.ToolInvocationResult;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.tool.repository.ToolDefinitionRepository;
import com.mcpgateway.domain.tool.repository.ToolExecutor;
import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import com.mcpgateway.domain.upstream.repository.UpstreamServerRepository;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.util.List;

public class GatewayToolService {

    private final ToolCatalogService toolCatalogService;
    private final ToolDefinitionRepository toolDefinitionRepository;
    private final UpstreamServerRepository upstreamServerRepository;
    private final ToolAccessPolicyService toolAccessPolicyService;
    private final ToolExecutor toolExecutor;

    public GatewayToolService(
            ToolCatalogService toolCatalogService,
            ToolDefinitionRepository toolDefinitionRepository,
            UpstreamServerRepository upstreamServerRepository,
            ToolAccessPolicyService toolAccessPolicyService,
            ToolExecutor toolExecutor
    ) {
        this.toolCatalogService = toolCatalogService;
        this.toolDefinitionRepository = toolDefinitionRepository;
        this.upstreamServerRepository = upstreamServerRepository;
        this.toolAccessPolicyService = toolAccessPolicyService;
        this.toolExecutor = toolExecutor;
    }

    public List<ToolDefinition> discover(String environment, GatewayClient client) {
        return toolCatalogService.listDiscoverableByEnvironment(environment).stream()
                .filter(tool -> toolAccessPolicyService.isAllowed(environment, client, tool.toolIdentifier()))
                .toList();
    }

    public ToolInvocationResult invoke(
            GatewayClient client,
            String requestId,
            String sessionId,
            ToolInvocationCommand command
    ) {
        ToolDefinition toolDefinition = toolDefinitionRepository.findByEnvironmentAndIdentifier(
                        command.environment(),
                        command.toolIdentifier()
                )
                .orElseThrow(() -> new AppException(
                        ResponseCode.NOT_FOUND,
                        "tool not found: " + command.toolIdentifier()
                ));
        UpstreamServer upstreamServer = upstreamServerRepository.findByEnvironmentAndServerCode(
                        command.environment(),
                        toolDefinition.serverCode()
                )
                .orElseThrow(() -> new AppException(
                        ResponseCode.NOT_FOUND,
                        "upstream server not found for tool: " + toolDefinition.serverCode()
                ));
        if (!upstreamServer.enabled() || upstreamServer.healthStatus() != HealthStatus.UP) {
            throw new AppException(
                    ResponseCode.CONFLICT,
                    "upstream server is not routable: " + upstreamServer.serverCode()
            );
        }
        toolAccessPolicyService.requireAllowed(command.environment(), client, toolDefinition.toolIdentifier());
        ToolExecutionResult executionResult = toolExecutor.execute(
                client,
                upstreamServer,
                toolDefinition,
                command.arguments(),
                requestId,
                sessionId
        );
        return new ToolInvocationResult(
                requestId,
                sessionId,
                client.clientId(),
                upstreamServer.serverCode(),
                toolDefinition.toolName(),
                toolDefinition.toolIdentifier(),
                executionResult.status(),
                executionResult.output()
        );
    }
}

