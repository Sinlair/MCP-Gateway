package com.mcpgateway.domain.upstream.model;

import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.net.URI;
import java.time.Instant;

public record UpstreamServer(
        String environment,
        String serverCode,
        String name,
        String baseUrl,
        TransportType transportType,
        String authMode,
        boolean enabled,
        int timeoutMs,
        HealthStatus healthStatus,
        Instant lastCheckedAt
) {

    public UpstreamServer {
        if (isBlank(environment)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "environment must not be blank");
        }
        if (isBlank(serverCode)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "serverCode must not be blank");
        }
        if (isBlank(name)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "name must not be blank");
        }
        if (transportType == null) {
            throw new AppException(ResponseCode.BAD_REQUEST, "transportType must not be null");
        }
        if (isBlank(authMode)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "authMode must not be blank");
        }
        if (timeoutMs <= 0) {
            throw new AppException(ResponseCode.BAD_REQUEST, "timeoutMs must be greater than 0");
        }
        validateBaseUrl(baseUrl);
    }

    public static UpstreamServer register(UpstreamRegistrationCommand command) {
        return new UpstreamServer(
                command.environment(),
                command.serverCode(),
                command.name(),
                command.baseUrl(),
                command.transportType(),
                command.authMode(),
                command.enabled(),
                command.timeoutMs(),
                HealthStatus.UNKNOWN,
                null
        );
    }

    public UpstreamServer withHealthStatus(HealthStatus newStatus, Instant checkedAt) {
        return new UpstreamServer(
                environment,
                serverCode,
                name,
                baseUrl,
                transportType,
                authMode,
                enabled,
                timeoutMs,
                newStatus,
                checkedAt
        );
    }

    private static void validateBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) {
            throw new AppException(ResponseCode.BAD_REQUEST, "baseUrl must not be blank");
        }
        URI uri = URI.create(baseUrl);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new AppException(ResponseCode.BAD_REQUEST, "baseUrl must be an absolute URL");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

