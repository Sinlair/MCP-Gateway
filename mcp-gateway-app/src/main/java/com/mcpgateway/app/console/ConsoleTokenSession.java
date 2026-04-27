package com.mcpgateway.app.console;

import com.mcpgateway.domain.security.model.GatewayClient;
import java.time.Instant;

public record ConsoleTokenSession(
        String accessToken,
        GatewayClient gatewayClient,
        String environment,
        Instant issuedAt,
        Instant expiresAt
) {

    public boolean expired(Instant now) {
        return expiresAt.isBefore(now);
    }
}

