package com.mcpgateway.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.service.ClientAuthenticationService;
import com.mcpgateway.types.context.GatewayRequestContext;
import com.mcpgateway.types.context.RequestAttributeNames;
import com.mcpgateway.types.context.ResponseContext;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import com.mcpgateway.types.response.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayRequestFilter extends OncePerRequestFilter {

    private final ClientAuthenticationService clientAuthenticationService;
    private final ObjectMapper objectMapper;

    public GatewayRequestFilter(
            ClientAuthenticationService clientAuthenticationService,
            ObjectMapper objectMapper
    ) {
        this.clientAuthenticationService = clientAuthenticationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        String sessionId = firstNonBlank(request.getHeader("X-Session-Id"), UUID.randomUUID().toString());
        ResponseContext.setRequestId(requestId);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-Session-Id", sessionId);
        try {
            GatewayClient client = authenticate(request);
            if (request.getRequestURI().startsWith("/api/v1/admin/")) {
                clientAuthenticationService.requireAdmin(client);
            }
            GatewayRequestContext requestContext = new GatewayRequestContext(requestId, sessionId, client.clientId());
            request.setAttribute(RequestAttributeNames.REQUEST_CONTEXT, requestContext);
            request.setAttribute(RequestAttributeNames.AUTHENTICATED_CLIENT, client);
            response.setHeader("X-Client-Id", client.clientId());
            filterChain.doFilter(request, response);
        } catch (AppException exception) {
            writeError(response, resolveStatus(exception.getCode()), Result.failure(exception.getCode(), exception.getMessage()));
        } catch (Exception exception) {
            writeError(response, HttpStatus.INTERNAL_SERVER_ERROR, Result.failure(ResponseCode.SYSTEM_ERROR, exception.getMessage()));
        } finally {
            ResponseContext.clear();
        }
    }

    private GatewayClient authenticate(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        String bearerToken = extractBearerToken(request.getHeader("Authorization"));
        if (isBlank(apiKey) && isBlank(bearerToken)) {
            throw new AppException(ResponseCode.UNAUTHORIZED, "missing credentials");
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

    private HttpStatus resolveStatus(String code) {
        if (ResponseCode.BAD_REQUEST.code().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ResponseCode.UNAUTHORIZED.code().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ResponseCode.FORBIDDEN.code().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ResponseCode.NOT_FOUND.code().equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (ResponseCode.CONFLICT.code().equals(code)) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

