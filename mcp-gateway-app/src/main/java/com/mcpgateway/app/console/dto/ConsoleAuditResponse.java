package com.mcpgateway.app.console.dto;

import java.time.Instant;
import java.util.List;

public record ConsoleAuditResponse(
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
