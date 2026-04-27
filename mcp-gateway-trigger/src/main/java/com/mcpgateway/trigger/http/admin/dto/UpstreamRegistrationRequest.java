package com.mcpgateway.trigger.http.admin.dto;

import com.mcpgateway.domain.upstream.model.TransportType;
import com.mcpgateway.domain.upstream.model.UpstreamRegistrationCommand;

public record UpstreamRegistrationRequest(
        String environment,
        String serverCode,
        String name,
        String baseUrl,
        TransportType transportType,
        String authMode,
        boolean enabled,
        int timeoutMs
) {

    public UpstreamRegistrationCommand toCommand() {
        return new UpstreamRegistrationCommand(
                environment,
                serverCode,
                name,
                baseUrl,
                transportType,
                authMode,
                enabled,
                timeoutMs
        );
    }
}

