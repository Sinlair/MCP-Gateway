package com.mcpgateway.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mcpgateway.domain.accesscontrol.model.PolicyDecision;
import com.mcpgateway.domain.accesscontrol.model.PolicySubjectType;
import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicy;
import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicyCommand;
import com.mcpgateway.domain.accesscontrol.repository.ToolAccessPolicyRepository;
import com.mcpgateway.domain.accesscontrol.service.ToolAccessPolicyService;
import com.mcpgateway.domain.gateway.model.ToolExecutionResult;
import com.mcpgateway.domain.gateway.model.ToolInvocationCommand;
import com.mcpgateway.domain.gateway.model.ToolInvocationResult;
import com.mcpgateway.domain.security.model.ClientRole;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.tool.repository.ToolDefinitionRepository;
import com.mcpgateway.domain.tool.repository.ToolExecutor;
import com.mcpgateway.domain.tool.service.GatewayToolService;
import com.mcpgateway.domain.tool.service.ToolCatalogService;
import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.model.TransportType;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import com.mcpgateway.domain.upstream.repository.UpstreamServerRepository;
import com.mcpgateway.types.exception.AppException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GatewayToolServiceTest {

    @Test
    void shouldThrowNotFoundWhenToolIsMissing() {
        InMemoryToolDefinitionRepository toolRepository = new InMemoryToolDefinitionRepository();
        InMemoryUpstreamServerRepository upstreamRepository = new InMemoryUpstreamServerRepository();
        ToolAccessPolicyService policyService = new ToolAccessPolicyService(new InMemoryPolicyRepository());
        GatewayToolService service = new GatewayToolService(
                new ToolCatalogService(toolRepository, upstreamRepository),
                toolRepository,
                upstreamRepository,
                policyService,
                new StubToolExecutor()
        );

        assertThrows(
                AppException.class,
                () -> service.invoke(client(), "req-1", "session-1", new ToolInvocationCommand("dev", "missing:tool", Map.of()))
        );
    }

    @Test
    void shouldThrowConflictWhenUpstreamIsNotRoutable() {
        InMemoryToolDefinitionRepository toolRepository = new InMemoryToolDefinitionRepository();
        InMemoryUpstreamServerRepository upstreamRepository = new InMemoryUpstreamServerRepository();
        ToolAccessPolicyService policyService = new ToolAccessPolicyService(new InMemoryPolicyRepository());
        GatewayToolService service = new GatewayToolService(
                new ToolCatalogService(toolRepository, upstreamRepository),
                toolRepository,
                upstreamRepository,
                policyService,
                new StubToolExecutor()
        );

        toolRepository.save(new ToolDefinition("dev", "weather", "forecast", "desc", "{}", true));
        upstreamRepository.save(new UpstreamServer(
                "dev",
                "weather",
                "Weather",
                "https://weather.example.com/mcp",
                TransportType.HTTP,
                "API_KEY",
                true,
                3000,
                HealthStatus.UNKNOWN,
                Instant.now()
        ));

        assertThrows(
                AppException.class,
                () -> service.invoke(client(), "req-2", "session-2", new ToolInvocationCommand("dev", "weather:forecast", Map.of()))
        );
    }

    @Test
    void shouldInvokeToolWhenAllowedAndUpstreamUp() {
        InMemoryToolDefinitionRepository toolRepository = new InMemoryToolDefinitionRepository();
        InMemoryUpstreamServerRepository upstreamRepository = new InMemoryUpstreamServerRepository();
        InMemoryPolicyRepository policyRepository = new InMemoryPolicyRepository();
        ToolAccessPolicyService policyService = new ToolAccessPolicyService(policyRepository);
        GatewayToolService service = new GatewayToolService(
                new ToolCatalogService(toolRepository, upstreamRepository),
                toolRepository,
                upstreamRepository,
                policyService,
                new StubToolExecutor()
        );
        GatewayClient client = client();
        toolRepository.save(new ToolDefinition("dev", "weather", "forecast", "desc", "{}", true));
        upstreamRepository.save(new UpstreamServer(
                "dev",
                "weather",
                "Weather",
                "https://weather.example.com/mcp",
                TransportType.HTTP,
                "API_KEY",
                true,
                3000,
                HealthStatus.UP,
                Instant.now()
        ));
        policyService.register(new ToolAccessPolicyCommand(
                "dev",
                PolicySubjectType.CLIENT,
                client.clientId(),
                "weather:forecast",
                PolicyDecision.ALLOW,
                true,
                "allow test"
        ));

        ToolInvocationResult result = service.invoke(
                client,
                "req-3",
                "session-3",
                new ToolInvocationCommand("dev", "weather:forecast", Map.of("city", "Shanghai"))
        );

        assertEquals("SUCCESS", result.status());
        assertEquals("weather:forecast", result.toolIdentifier());
    }

    private GatewayClient client() {
        return new GatewayClient(
                "demo-app",
                "Demo App",
                "app-key",
                "app-token",
                Set.of(ClientRole.APP),
                true
        );
    }

    private static final class StubToolExecutor implements ToolExecutor {

        @Override
        public ToolExecutionResult execute(
                GatewayClient client,
                UpstreamServer upstreamServer,
                ToolDefinition toolDefinition,
                Map<String, Object> arguments,
                String requestId,
                String sessionId
        ) {
            return new ToolExecutionResult("SUCCESS", Map.of(
                    "requestId", requestId,
                    "sessionId", sessionId,
                    "toolIdentifier", toolDefinition.toolIdentifier()
            ));
        }
    }

    private static final class InMemoryToolDefinitionRepository implements ToolDefinitionRepository {

        private final List<ToolDefinition> items = new ArrayList<>();

        @Override
        public ToolDefinition save(ToolDefinition toolDefinition) {
            items.removeIf(existing ->
                    existing.environment().equals(toolDefinition.environment())
                            && existing.toolIdentifier().equals(toolDefinition.toolIdentifier()));
            items.add(toolDefinition);
            return toolDefinition;
        }

        @Override
        public List<ToolDefinition> findAllByEnvironment(String environment) {
            return items.stream().filter(item -> item.environment().equals(environment)).toList();
        }

        @Override
        public List<ToolDefinition> findAllByEnvironmentAndServerCode(String environment, String serverCode) {
            return items.stream()
                    .filter(item -> item.environment().equals(environment) && item.serverCode().equals(serverCode))
                    .toList();
        }

        @Override
        public Optional<ToolDefinition> findByEnvironmentAndIdentifier(String environment, String toolIdentifier) {
            return items.stream()
                    .filter(item -> item.environment().equals(environment) && item.toolIdentifier().equals(toolIdentifier))
                    .findFirst();
        }
    }

    private static final class InMemoryUpstreamServerRepository implements UpstreamServerRepository {

        private final List<UpstreamServer> items = new ArrayList<>();

        @Override
        public UpstreamServer save(UpstreamServer upstreamServer) {
            items.removeIf(existing ->
                    existing.environment().equals(upstreamServer.environment())
                            && existing.serverCode().equals(upstreamServer.serverCode()));
            items.add(upstreamServer);
            return upstreamServer;
        }

        @Override
        public Optional<UpstreamServer> findByEnvironmentAndServerCode(String environment, String serverCode) {
            return items.stream()
                    .filter(item -> item.environment().equals(environment) && item.serverCode().equals(serverCode))
                    .findFirst();
        }

        @Override
        public List<UpstreamServer> findAllByEnvironment(String environment) {
            return items.stream().filter(item -> item.environment().equals(environment)).toList();
        }
    }

    private static final class InMemoryPolicyRepository implements ToolAccessPolicyRepository {

        private final List<ToolAccessPolicy> items = new ArrayList<>();

        @Override
        public ToolAccessPolicy save(ToolAccessPolicy toolAccessPolicy) {
            items.add(toolAccessPolicy);
            return toolAccessPolicy;
        }

        @Override
        public List<ToolAccessPolicy> findAllByEnvironment(String environment) {
            return items.stream().filter(item -> item.environment().equals(environment)).toList();
        }
    }
}
