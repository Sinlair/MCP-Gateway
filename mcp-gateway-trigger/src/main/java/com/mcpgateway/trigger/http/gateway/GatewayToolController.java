package com.mcpgateway.trigger.http.gateway;

import com.mcpgateway.domain.gateway.model.ToolInvocationResult;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.service.GatewayToolService;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.trigger.http.admin.tool.dto.ToolDefinitionResponse;
import com.mcpgateway.trigger.http.gateway.dto.ToolInvocationRequest;
import com.mcpgateway.trigger.http.gateway.dto.ToolInvocationResponse;
import com.mcpgateway.types.context.GatewayRequestContext;
import com.mcpgateway.types.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gateway/tools")
public class GatewayToolController {

    private final GatewayToolService gatewayToolService;

    public GatewayToolController(GatewayToolService gatewayToolService) {
        this.gatewayToolService = gatewayToolService;
    }

    @GetMapping
    public Result<List<ToolDefinitionResponse>> discover(
            @RequestParam(defaultValue = "dev") String environment,
            HttpServletRequest request
    ) {
        GatewayClient client = RequestSupport.requiredClient(request);
        return Result.success(gatewayToolService.discover(environment, client).stream()
                .map(ToolDefinitionResponse::from)
                .toList());
    }

    @PostMapping("/invoke")
    public Result<ToolInvocationResponse> invoke(
            @RequestBody ToolInvocationRequest requestBody,
            HttpServletRequest request
    ) {
        GatewayClient client = RequestSupport.requiredClient(request);
        GatewayRequestContext requestContext = RequestSupport.requiredContext(request);
        ToolInvocationResult result = gatewayToolService.invoke(
                client,
                requestContext.requestId(),
                requestContext.sessionId(),
                requestBody.toCommand()
        );
        return Result.success(ToolInvocationResponse.from(result));
    }
}

