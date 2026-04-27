package com.mcpgateway.infrastructure.security.repository;

import com.mcpgateway.domain.security.model.ClientRole;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.repository.GatewayClientRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryGatewayClientRepository implements GatewayClientRepository {

    private final List<GatewayClient> clients = List.of(
            new GatewayClient(
                    "demo-admin",
                    "Demo Admin",
                    "demo-admin-key",
                    "demo-admin-token",
                    Set.of(ClientRole.ADMIN, ClientRole.APP),
                    true
            ),
            new GatewayClient(
                    "demo-app",
                    "Demo Application",
                    "demo-app-key",
                    "demo-app-token",
                    Set.of(ClientRole.APP),
                    true
            )
    );

    @Override
    public Optional<GatewayClient> findByApiKey(String apiKey) {
        return clients.stream().filter(client -> client.apiKey().equals(apiKey)).findFirst();
    }

    @Override
    public Optional<GatewayClient> findByBearerToken(String bearerToken) {
        return clients.stream().filter(client -> client.bearerToken().equals(bearerToken)).findFirst();
    }

    @Override
    public List<GatewayClient> findAll() {
        return clients;
    }
}

