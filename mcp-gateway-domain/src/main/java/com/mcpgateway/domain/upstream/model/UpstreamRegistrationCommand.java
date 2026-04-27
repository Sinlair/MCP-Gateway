package com.mcpgateway.domain.upstream.model;

public record UpstreamRegistrationCommand(
        String environment,
        String serverCode,
        String name,
        String baseUrl,
        TransportType transportType,
        String authMode,
        boolean enabled,
        int timeoutMs
) {
}

