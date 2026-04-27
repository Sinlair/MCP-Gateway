package com.mcpgateway.trigger.http.gateway;

import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.service.GatewayToolService;
import com.mcpgateway.domain.upstream.service.UpstreamRegistrationService;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.trigger.http.admin.dto.UpstreamServerResponse;
import com.mcpgateway.trigger.http.gateway.dto.GatewayOverviewResponse;
import com.mcpgateway.types.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayOverviewController {

    private final UpstreamRegistrationService upstreamRegistrationService;
    private final GatewayToolService gatewayToolService;

    public GatewayOverviewController(
            UpstreamRegistrationService upstreamRegistrationService,
            GatewayToolService gatewayToolService
    ) {
        this.upstreamRegistrationService = upstreamRegistrationService;
        this.gatewayToolService = gatewayToolService;
    }

    @GetMapping("/overview")
    public Result<GatewayOverviewResponse> overview(
            @RequestParam(defaultValue = "dev") String environment,
            HttpServletRequest request
    ) {
        GatewayClient client = RequestSupport.requiredClient(request);
        List<UpstreamServerResponse> enabledServers = upstreamRegistrationService.listEnabledByEnvironment(environment)
                .stream()
                .map(UpstreamServerResponse::from)
                .toList();
        GatewayOverviewResponse response = new GatewayOverviewResponse(
                environment,
                client.clientId(),
                upstreamRegistrationService.listByEnvironment(environment).size(),
                enabledServers.size(),
                upstreamRegistrationService.listRoutableByEnvironment(environment).size(),
                gatewayToolService.discover(environment, client).size(),
                List.of("upstream-registry", "session", "tool-catalog", "routing", "access-control"),
                enabledServers
        );
        return Result.success(response);
    }
}
