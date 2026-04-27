package com.mcpgateway.app.console;

import com.mcpgateway.domain.security.model.GatewayClient;
import java.time.Instant;

public record ConsoleTokenSession(
        String accessToken,
        String tokenId,
        GatewayClient gatewayClient,
        String environment,
        java.util.List<String> scopes,
        java.util.List<String> managedSystems,
        Instant issuedAt,
        Instant expiresAt
) {

    public boolean expired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }

    public boolean managesSystem(String systemName) {
        return managedSystems.contains(systemName);
    }
}
