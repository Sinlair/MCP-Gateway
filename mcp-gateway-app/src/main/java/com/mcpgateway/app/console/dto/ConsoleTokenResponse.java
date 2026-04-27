package com.mcpgateway.app.console.dto;

import java.time.Instant;
import java.util.List;

public record ConsoleTokenResponse(
        String accessToken,
        String tokenType,
        String profile,
        String environment,
        Instant issuedAt,
        Instant expiresAt,
        List<String> roles
) {
}

