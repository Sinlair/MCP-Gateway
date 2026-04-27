package com.mcpgateway.app.console;

import java.time.Instant;
import java.util.List;

public record ConsoleAuditEvent(
        String eventId,
        String eventType,
        String tokenId,
        String profile,
        String environment,
        String requestPath,
        String outcome,
        String message,
        Instant occurredAt,
        List<String> scopes,
        List<String> managedSystems
) {
}

