package com.mcpgateway.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpgateway.app.console.ConsoleTokenService;
import com.mcpgateway.app.console.ConsoleTokenSession;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.service.ClientAuthenticationService;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.trigger.http.ErrorResponseSupport;
import com.mcpgateway.types.context.GatewayRequestContext;
import com.mcpgateway.types.context.RequestAttributeNames;
import com.mcpgateway.types.context.ResponseContext;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import com.mcpgateway.types.response.Result;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayRequestFilter.class);

    private final ClientAuthenticationService clientAuthenticationService;
    private final ConsoleTokenService consoleTokenService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public GatewayRequestFilter(
            ClientAuthenticationService clientAuthenticationService,
            ConsoleTokenService consoleTokenService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.clientAuthenticationService = clientAuthenticationService;
        this.consoleTokenService = consoleTokenService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().startsWith("/api/v1/public/console/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        String sessionId = firstNonBlank(request.getHeader("X-Session-Id"), UUID.randomUUID().toString());
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        String clientId = "anonymous";
        ResponseContext.setRequestId(requestId);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-Session-Id", sessionId);
        try {
            GatewayClient client = authenticate(request);
            clientId = client.clientId();
            if (request.getRequestURI().startsWith("/api/v1/admin/")
                    && !RequestSupport.hasConsoleToken(request)) {
                clientAuthenticationService.requireAdmin(client);
            }
            GatewayRequestContext requestContext = new GatewayRequestContext(requestId, sessionId, client.clientId());
            request.setAttribute(RequestAttributeNames.REQUEST_CONTEXT, requestContext);
            request.setAttribute(RequestAttributeNames.AUTHENTICATED_CLIENT, client);
            response.setHeader("X-Client-Id", client.clientId());
            filterChain.doFilter(request, response);
        } catch (AppException exception) {
            ErrorResponseSupport.ResultCode resultCode = ErrorResponseSupport.resolveResultCode(exception.getCode());
            writeError(
                    response,
                    ErrorResponseSupport.resolveStatus(resultCode.code()),
                    Result.failure(resultCode.code(), resultCode.message())
            );
            meterRegistry.counter(
                    "mcp.gateway.request.failures",
                    "path", requestPath,
                    "method", method,
                    "code", resultCode.code()
            ).increment();
            log.warn(
                    "gateway.request_failed requestId={} sessionId={} clientId={} path={} method={} code={} detail={}",
                    requestId,
                    sessionId,
                    clientId,
                    requestPath,
                    method,
                    resultCode.code(),
                    exception.getMessage()
            );
        } catch (Exception exception) {
            writeError(
                    response,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    Result.failure(ResponseCode.SYSTEM_ERROR, ResponseCode.SYSTEM_ERROR.message())
            );
            meterRegistry.counter(
                    "mcp.gateway.request.failures",
                    "path", requestPath,
                    "method", method,
                    "code", ResponseCode.SYSTEM_ERROR.code()
            ).increment();
            log.error(
                    "gateway.request_failed requestId={} sessionId={} clientId={} path={} method={} code={} detail={}",
                    requestId,
                    sessionId,
                    clientId,
                    requestPath,
                    method,
                    ResponseCode.SYSTEM_ERROR.code(),
                    exception.getMessage(),
                    exception
            );
        } finally {
            long durationNanos = System.nanoTime() - startedAt;
            String status = String.valueOf(response.getStatus());
            Timer.builder("mcp.gateway.request.duration")
                    .tag("path", requestPath)
                    .tag("method", method)
                    .tag("status", status)
                    .register(meterRegistry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
            meterRegistry.counter(
                    "mcp.gateway.request.total",
                    "path", requestPath,
                    "method", method,
                    "status", status
            ).increment();
            log.info(
                    "gateway.request_completed requestId={} sessionId={} clientId={} path={} method={} status={} durationMs={}",
                    requestId,
                    sessionId,
                    clientId,
                    requestPath,
                    method,
                    status,
                    TimeUnit.NANOSECONDS.toMillis(durationNanos)
            );
            ResponseContext.clear();
        }
    }

    private GatewayClient authenticate(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        String bearerToken = extractBearerToken(request.getHeader("Authorization"));
        if (isBlank(apiKey) && isBlank(bearerToken)) {
            throw new AppException(ResponseCode.UNAUTHORIZED, "missing credentials");
        }
        if (!isBlank(bearerToken) && consoleTokenService.looksLikeConsoleToken(bearerToken)) {
            ConsoleTokenSession consoleSession = consoleTokenService.authenticateConsoleToken(
                    bearerToken,
                    request.getRequestURI()
            );
            request.setAttribute(RequestAttributeNames.CONSOLE_TOKEN_SESSION, consoleSession);
            request.setAttribute(RequestAttributeNames.CONSOLE_TOKEN_ID, consoleSession.tokenId());
            request.setAttribute(RequestAttributeNames.CONSOLE_TOKEN_ENVIRONMENT, consoleSession.environment());
            request.setAttribute(RequestAttributeNames.CONSOLE_TOKEN_SCOPES, consoleSession.scopes());
            request.setAttribute(RequestAttributeNames.CONSOLE_TOKEN_MANAGED_SYSTEMS, consoleSession.managedSystems());
            return consoleSession.gatewayClient();
        }
        return clientAuthenticationService.authenticate(apiKey, bearerToken);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, Result<Void> body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
