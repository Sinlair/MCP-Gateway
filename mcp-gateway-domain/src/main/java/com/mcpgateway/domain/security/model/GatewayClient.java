package com.mcpgateway.domain.security.model;

import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.util.Set;

public record GatewayClient(
        String clientId,
        String clientName,
        String apiKey,
        String bearerToken,
        Set<ClientRole> roles,
        boolean enabled
) {

    public GatewayClient {
        if (isBlank(clientId)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "clientId must not be blank");
        }
        if (isBlank(clientName)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "clientName must not be blank");
        }
        if ((isBlank(apiKey) && isBlank(bearerToken)) || roles == null || roles.isEmpty()) {
            throw new AppException(ResponseCode.BAD_REQUEST, "client credentials and roles must be provided");
        }
    }

    public boolean hasRole(ClientRole role) {
        return roles.contains(role);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

