package com.mcpgateway.app.console.dto;

public record ConsoleTokenIssueRequest(
        String profile,
        String environment
) {
}

