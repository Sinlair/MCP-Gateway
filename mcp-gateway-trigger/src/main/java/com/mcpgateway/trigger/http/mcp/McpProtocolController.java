package com.mcpgateway.trigger.http.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.service.ClientAuthenticationService;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping
public class McpProtocolController {

    private static final String SESSION_COOKIE_NAME = "MCP_SESSION_ID";

    private final ClientAuthenticationService clientAuthenticationService;
    private final McpSseSessionRegistry sessionRegistry;
    private final McpProtocolService mcpProtocolService;

    public McpProtocolController(
            ClientAuthenticationService clientAuthenticationService,
            McpSseSessionRegistry sessionRegistry,
            McpProtocolService mcpProtocolService
    ) {
        this.clientAuthenticationService = clientAuthenticationService;
        this.sessionRegistry = sessionRegistry;
        this.mcpProtocolService = mcpProtocolService;
    }

    @GetMapping(value = "/mcp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openMcpStream(
            @RequestParam(value = "environment", defaultValue = "dev") String environment,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestParam(value = "access_token", required = false) String accessToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        GatewayClient client = authenticate(request, apiKey, accessToken);
        McpSseSession session = sessionRegistry.openSession(client, environment);

        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(SESSION_COOKIE_NAME, session.sessionId())
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .build()
                .toString());
        response.addHeader("X-Session-Id", session.sessionId());
        response.addHeader("X-Client-Id", client.clientId());

        try {
            sessionRegistry.sendEndpoint(session, buildEndpoint(request, session));
        } catch (IOException exception) {
            session.emitter().completeWithError(exception);
            throw exception;
        }

        return session.emitter();
    }

    @PostMapping(value = {"/mcp", "/mcp/message"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleMessage(
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "environment", required = false) String environment,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestParam(value = "access_token", required = false) String accessToken,
            @RequestBody JsonNode payload,
            HttpServletRequest request
    ) throws IOException {
        McpSseSession session = resolveSession(sessionId, request);
        if (session == null) {
            throw new AppException(ResponseCode.BAD_REQUEST, "missing MCP session, connect to /mcp first");
        }

        GatewayClient client = session.client();
        if (client == null) {
            client = authenticate(request, apiKey, accessToken);
        }

        String resolvedEnvironment = environment == null || environment.isBlank()
                ? session.environment()
                : environment;
        String requestId = UUID.randomUUID().toString();
        JsonNode responsePayload = mcpProtocolService.handleRequest(
                client,
                resolvedEnvironment,
                session.sessionId(),
                requestId,
                payload
        );

        if (responsePayload != null) {
            sessionRegistry.sendMessage(session, responsePayload);
        }

        return ResponseEntity.accepted()
                .header("X-Request-Id", requestId)
                .header("X-Session-Id", session.sessionId())
                .header("X-Client-Id", client.clientId())
                .build();
    }

    private GatewayClient authenticate(HttpServletRequest request, String apiKeyQuery, String accessTokenQuery) {
        String apiKey = firstNonBlank(apiKeyQuery, request.getHeader("X-API-Key"));
        String bearerToken = firstNonBlank(
                accessTokenQuery,
                extractBearerToken(request.getHeader("Authorization"))
        );
        if (isBlank(apiKey) && isBlank(bearerToken)) {
            throw new AppException(ResponseCode.UNAUTHORIZED, "missing credentials");
        }
        return clientAuthenticationService.authenticate(apiKey, bearerToken);
    }

    private McpSseSession resolveSession(String sessionId, HttpServletRequest request) {
        String resolvedSessionId = firstNonBlank(
                sessionId,
                request.getHeader("X-Session-Id"),
                readCookie(request, SESSION_COOKIE_NAME).orElse(null)
        );
        return sessionRegistry.find(resolvedSessionId).orElse(null);
    }

    private String buildEndpoint(HttpServletRequest request, McpSseSession session) {
        String basePath = request.getContextPath() == null ? "" : request.getContextPath();
        String encodedEnvironment = URLEncoder.encode(session.environment(), StandardCharsets.UTF_8);
        return basePath + "/mcp/message?sessionId=" + session.sessionId() + "&environment=" + encodedEnvironment;
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
