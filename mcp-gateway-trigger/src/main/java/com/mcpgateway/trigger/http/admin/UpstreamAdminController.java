package com.mcpgateway.trigger.http.admin;

import com.mcpgateway.domain.upstream.service.UpstreamRegistrationService;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.trigger.http.admin.dto.UpstreamRegistrationRequest;
import com.mcpgateway.trigger.http.admin.dto.UpstreamServerResponse;
import com.mcpgateway.types.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/upstreams")
public class UpstreamAdminController {

    private final UpstreamRegistrationService upstreamRegistrationService;

    public UpstreamAdminController(UpstreamRegistrationService upstreamRegistrationService) {
        this.upstreamRegistrationService = upstreamRegistrationService;
    }

    @PostMapping
    public Result<UpstreamServerResponse> register(
            @RequestBody UpstreamRegistrationRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "upstream:manage");
        RequestSupport.requireEnvironment(servletRequest, request.environment());
        return Result.success(UpstreamServerResponse.from(upstreamRegistrationService.register(request.toCommand())));
    }

    @GetMapping
    public Result<List<UpstreamServerResponse>> list(
            @RequestParam(defaultValue = "dev") String environment,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "upstream:manage");
        RequestSupport.requireEnvironment(servletRequest, environment);
        return Result.success(upstreamRegistrationService.listByEnvironment(environment).stream()
                .map(UpstreamServerResponse::from)
                .toList());
    }

    @PostMapping("/{serverCode}/refresh")
    public Result<UpstreamServerResponse> refresh(
            @PathVariable String serverCode,
            @RequestParam(defaultValue = "dev") String environment,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "upstream:manage");
        RequestSupport.requireEnvironment(servletRequest, environment);
        return Result.success(UpstreamServerResponse.from(
                upstreamRegistrationService.refreshStatus(environment, serverCode)
        ));
    }
}
