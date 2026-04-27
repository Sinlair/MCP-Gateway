package com.mcpgateway.app.console;

import java.time.Instant;
import java.util.List;

public record ConsoleTokenClaims(
        String tokenId,
        String subject,
        String environment,
        Instant issuedAt,
        Instant expiresAt,
        List<String> roles,
        List<String> scopes,
        List<String> managedSystems
) {
}
