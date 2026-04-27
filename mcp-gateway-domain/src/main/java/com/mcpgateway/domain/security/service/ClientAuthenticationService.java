package com.mcpgateway.domain.security.service;

import com.mcpgateway.domain.security.model.ClientRole;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.repository.GatewayClientRepository;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;

public class ClientAuthenticationService {

    private final GatewayClientRepository gatewayClientRepository;

    public ClientAuthenticationService(GatewayClientRepository gatewayClientRepository) {
        this.gatewayClientRepository = gatewayClientRepository;
    }

    public GatewayClient authenticate(String apiKey, String bearerToken) {
        GatewayClient client = isBlank(apiKey)
                ? gatewayClientRepository.findByBearerToken(bearerToken).orElseThrow(() ->
                        new AppException(ResponseCode.UNAUTHORIZED, "invalid bearer token"))
                : gatewayClientRepository.findByApiKey(apiKey).orElseThrow(() ->
                        new AppException(ResponseCode.UNAUTHORIZED, "invalid api key"));
        if (!client.enabled()) {
            throw new AppException(ResponseCode.FORBIDDEN, "client is disabled");
        }
        return client;
    }

    public void requireAdmin(GatewayClient client) {
        if (!client.hasRole(ClientRole.ADMIN)) {
            throw new AppException(ResponseCode.FORBIDDEN, "admin role is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

