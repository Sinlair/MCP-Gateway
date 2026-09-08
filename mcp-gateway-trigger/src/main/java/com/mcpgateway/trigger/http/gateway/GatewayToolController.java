package com.mcpgateway.trigger.http.gateway;

import com.mcpgateway.domain.gateway.model.ToolInvocationResult;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.service.GatewayToolService;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.trigger.http.admin.tool.dto.ToolDefinitionResponse;
import com.mcpgateway.trigger.http.gateway.dto.ToolInvocationRequest;
import com.mcpgateway.trigger.http.gateway.dto.ToolInvocationResponse;
import com.mcpgateway.types.context.GatewayRequestContext;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import com.mcpgateway.types.response.Result;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gateway/tools")
public class GatewayToolController {

    private static final Logger log = LoggerFactory.getLogger(GatewayToolController.class);

    private final GatewayToolService gatewayToolService;
    private final MeterRegistry meterRegistry;

    public GatewayToolController(GatewayToolService gatewayToolService, MeterRegistry meterRegistry) {
        this.gatewayToolService = gatewayToolService;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping
    public Result<List<ToolDefinitionResponse>> discover(
            @RequestParam(defaultValue = "dev") String environment,
            HttpServletRequest request
    ) {
        RequestSupport.requireAnyScope(request, "console:read");
        RequestSupport.requireEnvironment(request, environment);
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
        long startedAt = System.nanoTime();
        RequestSupport.requireAnyScope(request, "gateway:invoke");
        RequestSupport.requireEnvironment(request, requestBody.environment());
        GatewayClient client = RequestSupport.requiredClient(request);
        GatewayRequestContext requestContext = RequestSupport.requiredContext(request);
        try {
            ToolInvocationResult result = gatewayToolService.invoke(
                    client,
                    requestContext.requestId(),
                    requestContext.sessionId(),
                    requestBody.toCommand()
            );
            long durationNanos = System.nanoTime() - startedAt;
            Timer.builder("mcp.gateway.tool.invoke.duration")
                    .tag("environment", safeTag(requestBody.environment()))
                    .tag("toolIdentifier", safeTag(requestBody.toolIdentifier()))
                    .tag("status", result.status())
                    .register(meterRegistry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
            log.info(
                    "gateway.tool_invocation requestId={} sessionId={} clientId={} toolIdentifier={} status={} durationMs={}",
                    requestContext.requestId(),
                    requestContext.sessionId(),
                    client.clientId(),
                    requestBody.toolIdentifier(),
                    result.status(),
                    TimeUnit.NANOSECONDS.toMillis(durationNanos)
            );
            return Result.success(ToolInvocationResponse.from(result));
        } catch (AppException exception) {
            meterRegistry.counter(
                    "mcp.gateway.tool.invoke.failures",
                    "environment", safeTag(requestBody.environment()),
                    "toolIdentifier", safeTag(requestBody.toolIdentifier()),
                    "code", exception.getCode()
            ).increment();
            throw exception;
        } catch (Exception exception) {
            meterRegistry.counter(
                    "mcp.gateway.tool.invoke.failures",
                    "environment", safeTag(requestBody.environment()),
                    "toolIdentifier", safeTag(requestBody.toolIdentifier()),
                    "code", ResponseCode.SYSTEM_ERROR.code()
            ).increment();
            throw exception;
        }
    }

    private String safeTag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
