package com.mcpgateway.trigger.http;

import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.types.context.GatewayRequestContext;
import com.mcpgateway.types.context.RequestAttributeNames;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public final class RequestSupport {

    private RequestSupport() {
    }

    public static GatewayClient requiredClient(HttpServletRequest request) {
        Object client = request.getAttribute(RequestAttributeNames.AUTHENTICATED_CLIENT);
        if (client instanceof GatewayClient gatewayClient) {
            return gatewayClient;
        }
        throw new AppException(ResponseCode.UNAUTHORIZED, "missing authenticated caller");
    }

    public static GatewayRequestContext requiredContext(HttpServletRequest request) {
        Object context = request.getAttribute(RequestAttributeNames.REQUEST_CONTEXT);
        if (context instanceof GatewayRequestContext gatewayRequestContext) {
            return gatewayRequestContext;
        }
        throw new AppException(ResponseCode.SYSTEM_ERROR, "missing request context");
    }

    public static boolean hasConsoleToken(HttpServletRequest request) {
        return request.getAttribute(RequestAttributeNames.CONSOLE_TOKEN_ID) instanceof String;
    }

    public static void requireAnyScope(HttpServletRequest request, String... scopes) {
        if (!hasConsoleToken(request)) {
            return;
        }
        List<String> tokenScopes = consoleScopes(request);
        for (String scope : scopes) {
            if (tokenScopes.contains(scope)) {
                return;
            }
        }
        throw new AppException(ResponseCode.FORBIDDEN, "当前访问令牌缺少所需 scope");
    }

    public static void requireManagedSystem(HttpServletRequest request, String systemName) {
        if (!hasConsoleToken(request)) {
            return;
        }
        if (!managedSystems(request).contains(systemName)) {
            throw new AppException(ResponseCode.FORBIDDEN, "当前访问令牌没有系统访问范围: " + systemName);
        }
    }

    public static void requireEnvironment(HttpServletRequest request, String environment) {
        if (!hasConsoleToken(request)) {
            return;
        }
        Object tokenEnvironment = request.getAttribute(RequestAttributeNames.CONSOLE_TOKEN_ENVIRONMENT);
        if (!(tokenEnvironment instanceof String value) || !value.equals(environment)) {
            throw new AppException(ResponseCode.FORBIDDEN, "当前访问令牌不允许访问环境: " + environment);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> consoleScopes(HttpServletRequest request) {
        Object scopes = request.getAttribute(RequestAttributeNames.CONSOLE_TOKEN_SCOPES);
        return scopes instanceof List<?> ? (List<String>) scopes : List.of();
    }

    @SuppressWarnings("unchecked")
    public static List<String> managedSystems(HttpServletRequest request) {
        Object systems = request.getAttribute(RequestAttributeNames.CONSOLE_TOKEN_MANAGED_SYSTEMS);
        return systems instanceof List<?> ? (List<String>) systems : List.of();
    }
}
