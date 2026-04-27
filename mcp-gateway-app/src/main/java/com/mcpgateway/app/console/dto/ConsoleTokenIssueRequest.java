package com.mcpgateway.app.console.dto;

public record ConsoleTokenIssueRequest(
        String profile,
        String environment,
        java.util.List<String> scopes,
        java.util.List<String> managedSystems,
        Long ttlHours
) {
}
