package com.mcpgateway.app.console;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mcpgateway.app.console.dto.ConsoleAuditResponse;
import com.mcpgateway.app.console.dto.ConsoleSessionResponse;
import com.mcpgateway.app.console.dto.ConsoleTokenIssueRequest;
import com.mcpgateway.app.console.dto.ConsoleTokenResponse;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.repository.GatewayClientRepository;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class ConsoleTokenService {

    private final GatewayClientRepository gatewayClientRepository;
    private final ConsoleTokenProperties consoleTokenProperties;
    private final Algorithm jwtAlgorithm;
    private final JWTVerifier jwtVerifier;

    private final ConcurrentMap<String, ConsoleTokenSession> activeTokenSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> revokedTokenIds = new ConcurrentHashMap<>();
    private final ArrayDeque<ConsoleAuditEvent> auditTrail = new ArrayDeque<>();

    public ConsoleTokenService(
            GatewayClientRepository gatewayClientRepository,
            ConsoleTokenProperties consoleTokenProperties
    ) {
        this.gatewayClientRepository = gatewayClientRepository;
        this.consoleTokenProperties = consoleTokenProperties;
        this.jwtAlgorithm = Algorithm.HMAC256(consoleTokenProperties.getSecret());
        this.jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer(consoleTokenProperties.getIssuer())
                .acceptLeeway(consoleTokenProperties.getClockSkewSeconds())
                .build();
    }

    public ConsoleTokenResponse issueDemoToken(ConsoleTokenIssueRequest request) {
        ConsoleTokenSession session = issueToken(request);
        recordAudit(
                "TOKEN_ISSUED",
                session,
                null,
                "SUCCESS",
                "控制台访问令牌已签发"
        );
        return toTokenResponse(session);
    }

    public ConsoleTokenSession authenticateConsoleToken(String accessToken, String requestPath) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AppException(ResponseCode.UNAUTHORIZED, "控制台访问令牌为空");
        }

        ConsoleTokenClaims claims = verifyAndDecode(accessToken);
        if (revokedTokenIds.containsKey(claims.tokenId())) {
            recordAudit(
                    "TOKEN_REJECTED",
                    null,
                    claims,
                    "REVOKED",
                    "控制台访问令牌已撤销",
                    requestPath
            );
            throw new AppException(ResponseCode.UNAUTHORIZED, "访问令牌已撤销，请重新签发");
        }

        GatewayClient gatewayClient = gatewayClientRepository.findAll().stream()
                .filter(client -> client.clientId().equals(claims.subject()))
                .findFirst()
                .orElseThrow(() -> new AppException(ResponseCode.UNAUTHORIZED, "访问令牌对应的调用方不存在"));

        if (!gatewayClient.enabled()) {
            throw new AppException(ResponseCode.FORBIDDEN, "访问令牌对应的调用方已禁用");
        }

        ConsoleTokenSession session = new ConsoleTokenSession(
                accessToken,
                claims.tokenId(),
                gatewayClient,
                claims.environment(),
                claims.scopes(),
                claims.managedSystems(),
                claims.issuedAt(),
                claims.expiresAt()
        );
        activeTokenSessions.put(claims.tokenId(), session);
        recordAudit(
                "TOKEN_ACCEPTED",
                session,
                null,
                "SUCCESS",
                "控制台访问令牌校验通过",
                requestPath
        );
        return session;
    }

    public ConsoleSessionResponse introspect(ConsoleTokenSession session) {
        recordAudit(
                "TOKEN_INTROSPECTED",
                session,
                null,
                "SUCCESS",
                "读取控制台令牌会话信息"
        );
        return new ConsoleSessionResponse(
                session.tokenId(),
                session.gatewayClient().clientId(),
                session.environment(),
                session.issuedAt(),
                session.expiresAt(),
                session.gatewayClient().roles().stream().map(Enum::name).toList(),
                session.scopes(),
                session.managedSystems()
        );
    }

    public void revoke(ConsoleTokenSession session, String reason) {
        revokedTokenIds.put(session.tokenId(), Instant.now());
        activeTokenSessions.remove(session.tokenId());
        recordAudit(
                "TOKEN_REVOKED",
                session,
                null,
                "SUCCESS",
                reason == null || reason.isBlank() ? "控制台访问令牌已撤销" : reason
        );
    }

    public List<ConsoleAuditResponse> listAudits(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, consoleTokenProperties.getAuditRetention()));
        return auditTrail.stream()
                .limit(safeLimit)
                .map(event -> new ConsoleAuditResponse(
                        event.eventId(),
                        event.eventType(),
                        event.tokenId(),
                        event.profile(),
                        event.environment(),
                        event.requestPath(),
                        event.outcome(),
                        event.message(),
                        event.occurredAt(),
                        event.scopes(),
                        event.managedSystems()
                ))
                .toList();
    }

    public boolean looksLikeConsoleToken(String token) {
        return token != null && token.chars().filter(ch -> ch == '.').count() == 2;
    }

    private ConsoleTokenSession issueToken(ConsoleTokenIssueRequest request) {
        String profile = normalizeProfile(request.profile());
        GatewayClient gatewayClient = gatewayClientRepository.findAll().stream()
                .filter(client -> client.clientId().equals(profile))
                .findFirst()
                .orElseThrow(() -> new AppException(ResponseCode.NOT_FOUND, "未找到调用方配置: " + profile));

        String environment = request.environment() == null || request.environment().isBlank()
                ? "dev"
                : request.environment();
        List<String> scopes = resolveScopes(gatewayClient, request.scopes());
        List<String> managedSystems = resolveManagedSystems(request.managedSystems());
        Instant issuedAt = Instant.now();
        long ttlHours = request.ttlHours() == null || request.ttlHours() <= 0
                ? consoleTokenProperties.getTtlHours()
                : Math.min(request.ttlHours(), consoleTokenProperties.getTtlHours());
        Instant expiresAt = issuedAt.plus(ttlHours, ChronoUnit.HOURS);
        String tokenId = UUID.randomUUID().toString();

        ConsoleTokenClaims claims = new ConsoleTokenClaims(
                tokenId,
                gatewayClient.clientId(),
                environment,
                issuedAt,
                expiresAt,
                gatewayClient.roles().stream().map(Enum::name).toList(),
                scopes,
                managedSystems
        );

        String accessToken = sign(claims);
        ConsoleTokenSession session = new ConsoleTokenSession(
                accessToken,
                tokenId,
                gatewayClient,
                environment,
                scopes,
                managedSystems,
                issuedAt,
                expiresAt
        );
        activeTokenSessions.put(tokenId, session);
        return session;
    }

    private ConsoleTokenClaims verifyAndDecode(String accessToken) {
        try {
            DecodedJWT decodedJWT = jwtVerifier.verify(accessToken);
            Instant issuedAt = decodedJWT.getIssuedAt().toInstant();
            Instant expiresAt = decodedJWT.getExpiresAt().toInstant();
            if (expiresAt.plusSeconds(consoleTokenProperties.getClockSkewSeconds()).isBefore(Instant.now())) {
                throw new AppException(ResponseCode.UNAUTHORIZED, "控制台访问令牌已过期");
            }

            return new ConsoleTokenClaims(
                    decodedJWT.getId(),
                    decodedJWT.getSubject(),
                    decodedJWT.getClaim("env").asString(),
                    issuedAt,
                    expiresAt,
                    readStringList(decodedJWT.getClaim("roles")),
                    readStringList(decodedJWT.getClaim("scopes")),
                    readStringList(decodedJWT.getClaim("systems"))
            );
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ResponseCode.UNAUTHORIZED, "控制台访问令牌解析失败");
        }
    }

    private String sign(ConsoleTokenClaims claims) {
        try {
            return JWT.create()
                    .withIssuer(consoleTokenProperties.getIssuer())
                    .withJWTId(claims.tokenId())
                    .withSubject(claims.subject())
                    .withIssuedAt(java.util.Date.from(claims.issuedAt()))
                    .withExpiresAt(java.util.Date.from(claims.expiresAt()))
                    .withClaim("env", claims.environment())
                    .withArrayClaim("roles", claims.roles().toArray(String[]::new))
                    .withArrayClaim("scopes", claims.scopes().toArray(String[]::new))
                    .withArrayClaim("systems", claims.managedSystems().toArray(String[]::new))
                    .sign(jwtAlgorithm);
        } catch (Exception e) {
            throw new AppException(ResponseCode.SYSTEM_ERROR, "控制台访问令牌签发失败");
        }
    }

    private List<String> resolveScopes(GatewayClient gatewayClient, List<String> requestedScopes) {
        List<String> allowedScopes = gatewayClient.hasRole(com.mcpgateway.domain.security.model.ClientRole.ADMIN)
                ? ConsoleScopes.DEMO_ADMIN_SCOPES
                : ConsoleScopes.DEMO_APP_SCOPES;
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            return allowedScopes;
        }
        List<String> scopes = new ArrayList<>(requestedScopes);
        if (!allowedScopes.containsAll(scopes)) {
            throw new AppException(ResponseCode.FORBIDDEN, "请求的 scope 超出当前调用方允许范围");
        }
        return scopes;
    }

    private List<String> resolveManagedSystems(List<String> requestedManagedSystems) {
        List<String> allowedSystems = List.of(ConsoleScopes.BIG_MARKET_SYSTEM);
        if (requestedManagedSystems == null || requestedManagedSystems.isEmpty()) {
            return allowedSystems;
        }
        List<String> managedSystems = new ArrayList<>(requestedManagedSystems);
        if (!allowedSystems.containsAll(managedSystems)) {
            throw new AppException(ResponseCode.FORBIDDEN, "请求的受管系统超出允许范围");
        }
        return managedSystems;
    }

    private List<String> readStringList(Claim claim) {
        List<String> values = claim.asList(String.class);
        return values == null ? List.of() : values;
    }

    private ConsoleTokenResponse toTokenResponse(ConsoleTokenSession session) {
        return new ConsoleTokenResponse(
                session.accessToken(),
                "Bearer",
                session.tokenId(),
                session.gatewayClient().clientId(),
                session.environment(),
                session.issuedAt(),
                session.expiresAt(),
                session.gatewayClient().roles().stream().map(Enum::name).toList(),
                session.scopes(),
                session.managedSystems()
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

    private void recordAudit(
            String eventType,
            ConsoleTokenSession session,
            ConsoleTokenClaims claims,
            String outcome,
            String message
    ) {
        recordAudit(eventType, session, claims, outcome, message, null);
    }

    private synchronized void recordAudit(
            String eventType,
            ConsoleTokenSession session,
            ConsoleTokenClaims claims,
            String outcome,
            String message,
            String requestPath
    ) {
        List<String> scopes = session != null ? session.scopes() : claims != null ? claims.scopes() : List.of();
        List<String> systems = session != null ? session.managedSystems() : claims != null ? claims.managedSystems() : List.of();
        ConsoleAuditEvent event = new ConsoleAuditEvent(
                UUID.randomUUID().toString(),
                eventType,
                session != null ? session.tokenId() : claims != null ? claims.tokenId() : null,
                session != null ? session.gatewayClient().clientId() : claims != null ? claims.subject() : null,
                session != null ? session.environment() : claims != null ? claims.environment() : null,
                requestPath,
                outcome,
                message,
                Instant.now(),
                scopes,
                systems
        );
        auditTrail.addFirst(event);
        while (auditTrail.size() > consoleTokenProperties.getAuditRetention()) {
            auditTrail.removeLast();
        }
    }
}
