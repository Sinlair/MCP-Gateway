package com.mcpgateway.trigger.http;

import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.types.context.GatewayRequestContext;
import com.mcpgateway.types.context.RequestAttributeNames;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;

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
}

