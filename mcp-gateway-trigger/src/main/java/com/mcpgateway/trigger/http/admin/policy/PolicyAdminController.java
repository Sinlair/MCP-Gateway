package com.mcpgateway.trigger.http.admin.policy;

import com.mcpgateway.domain.accesscontrol.service.ToolAccessPolicyService;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.trigger.http.admin.policy.dto.ToolAccessPolicyRequest;
import com.mcpgateway.trigger.http.admin.policy.dto.ToolAccessPolicyResponse;
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
@RequestMapping("/api/v1/admin/policies")
public class PolicyAdminController {

    private final ToolAccessPolicyService toolAccessPolicyService;

    public PolicyAdminController(ToolAccessPolicyService toolAccessPolicyService) {
        this.toolAccessPolicyService = toolAccessPolicyService;
    }

    @PostMapping
    public Result<ToolAccessPolicyResponse> register(
            @RequestBody ToolAccessPolicyRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "policies:manage");
        RequestSupport.requireEnvironment(servletRequest, request.environment());
        return Result.success(ToolAccessPolicyResponse.from(toolAccessPolicyService.register(request.toCommand())));
    }

    @GetMapping
    public Result<List<ToolAccessPolicyResponse>> list(
            @RequestParam(defaultValue = "dev") String environment,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "policies:manage");
        RequestSupport.requireEnvironment(servletRequest, environment);
        return Result.success(toolAccessPolicyService.listByEnvironment(environment).stream()
                .map(ToolAccessPolicyResponse::from)
                .toList());
    }
}
