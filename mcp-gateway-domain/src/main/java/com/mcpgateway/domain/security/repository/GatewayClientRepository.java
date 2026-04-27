package com.mcpgateway.domain.security.repository;

import com.mcpgateway.domain.security.model.GatewayClient;
import java.util.List;
import java.util.Optional;

public interface GatewayClientRepository {

    Optional<GatewayClient> findByApiKey(String apiKey);

    Optional<GatewayClient> findByBearerToken(String bearerToken);

    List<GatewayClient> findAll();
}

