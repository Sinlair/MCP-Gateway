package com.mcpgateway.app.console;

import com.mcpgateway.app.console.dto.ConsoleSessionResponse;
import com.mcpgateway.app.console.dto.ConsoleTokenIssueRequest;
import com.mcpgateway.app.console.dto.ConsoleTokenResponse;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.repository.GatewayClientRepository;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class ConsoleTokenService {

    private final GatewayClientRepository gatewayClientRepository;
    private final ConcurrentMap<String, ConsoleTokenSession> tokenStore = new ConcurrentHashMap<>();

    public ConsoleTokenService(GatewayClientRepository gatewayClientRepository) {
        this.gatewayClientRepository = gatewayClientRepository;
    }

    public ConsoleTokenResponse issueDemoToken(ConsoleTokenIssueRequest request) {
        String profile = normalizeProfile(request.profile());
        GatewayClient gatewayClient = gatewayClientRepository.findAll().stream()
                .filter(client -> client.clientId().equals(profile))
                .findFirst()
                .orElseThrow(() -> new AppException(ResponseCode.NOT_FOUND, "未找到调用方配置: " + profile));

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(12, ChronoUnit.HOURS);
        String accessToken = "mcp_" + UUID.randomUUID().toString().replace("-", "");
        String environment = request.environment() == null || request.environment().isBlank()
                ? "dev"
                : request.environment();

        tokenStore.put(accessToken, new ConsoleTokenSession(
                accessToken,
                gatewayClient,
                environment,
                issuedAt,
                expiresAt
        ));

        return toTokenResponse(accessToken);
    }

    public Optional<ConsoleTokenSession> resolve(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        ConsoleTokenSession session = tokenStore.get(accessToken);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expired(Instant.now())) {
            tokenStore.remove(accessToken);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public ConsoleSessionResponse introspect(String accessToken) {
        ConsoleTokenSession session = resolve(accessToken)
                .orElseThrow(() -> new AppException(ResponseCode.UNAUTHORIZED, "访问令牌无效或已过期"));
        return new ConsoleSessionResponse(
                session.gatewayClient().clientId(),
                session.environment(),
                session.issuedAt(),
                session.expiresAt(),
                session.gatewayClient().roles().stream().map(Enum::name).toList()
        );
    }

    private ConsoleTokenResponse toTokenResponse(String accessToken) {
        ConsoleTokenSession session = tokenStore.get(accessToken);
        return new ConsoleTokenResponse(
                session.accessToken(),
                "Bearer",
                session.gatewayClient().clientId(),
                session.environment(),
                session.issuedAt(),
                session.expiresAt(),
                session.gatewayClient().roles().stream().map(Enum::name).toList()
        );
    }

    private String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            return "demo-admin";
        }
        return switch (profile) {
            case "admin", "demo-admin" -> "demo-admin";
            case "app", "demo-app" -> "demo-app";
            default -> profile;
        };
    }
}

