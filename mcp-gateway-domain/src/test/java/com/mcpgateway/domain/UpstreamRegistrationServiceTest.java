package com.mcpgateway.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mcpgateway.domain.upstream.model.HealthStatus;
import com.mcpgateway.domain.upstream.model.TransportType;
import com.mcpgateway.domain.upstream.model.UpstreamRegistrationCommand;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import com.mcpgateway.domain.upstream.repository.UpstreamHealthProbe;
import com.mcpgateway.domain.upstream.repository.UpstreamServerRepository;
import com.mcpgateway.domain.upstream.service.UpstreamRegistrationService;
import com.mcpgateway.types.exception.AppException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpstreamRegistrationServiceTest {

    @Test
    void shouldRejectDuplicateServerCodeWithinEnvironment() {
        InMemoryRepository repository = new InMemoryRepository();
        UpstreamRegistrationService service = new UpstreamRegistrationService(repository, server -> HealthStatus.UP);
        UpstreamRegistrationCommand command = new UpstreamRegistrationCommand(
                "dev",
                "weather",
                "Weather Server",
                "https://example.com/mcp",
                TransportType.HTTP,
                "API_KEY",
                true,
                3000
        );

        service.register(command);

        assertThrows(AppException.class, () -> service.register(command));
    }

    @Test
    void shouldRefreshHealthStatusUsingProbe() {
        InMemoryRepository repository = new InMemoryRepository();
        UpstreamHealthProbe probe = server -> HealthStatus.UP;
        UpstreamRegistrationService service = new UpstreamRegistrationService(repository, probe);
        service.register(new UpstreamRegistrationCommand(
                "dev",
                "calendar",
                "Calendar Server",
                "https://calendar.example.com/mcp",
                TransportType.SSE,
                "BEARER",
                true,
                5000
        ));

        UpstreamServer refreshed = service.refreshStatus("dev", "calendar");

        assertEquals(HealthStatus.UP, refreshed.healthStatus());
    }

    private static final class InMemoryRepository implements UpstreamServerRepository {

        private final List<UpstreamServer> items = new ArrayList<>();

        @Override
        public UpstreamServer save(UpstreamServer upstreamServer) {
            items.removeIf(item -> item.environment().equals(upstreamServer.environment())
                    && item.serverCode().equals(upstreamServer.serverCode()));
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
            return items.stream()
                    .filter(item -> item.environment().equals(environment))
                    .toList();
        }
    }
}

