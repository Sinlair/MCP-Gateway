package com.mcpgateway.trigger.http.admin.tool;

import com.mcpgateway.domain.tool.service.ToolCatalogService;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.trigger.http.admin.tool.dto.ToolDefinitionResponse;
import com.mcpgateway.trigger.http.admin.tool.dto.ToolRegistrationRequest;
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
@RequestMapping("/api/v1/admin/tools")
public class ToolAdminController {

    private final ToolCatalogService toolCatalogService;

    public ToolAdminController(ToolCatalogService toolCatalogService) {
        this.toolCatalogService = toolCatalogService;
    }

    @PostMapping
    public Result<ToolDefinitionResponse> register(
            @RequestBody ToolRegistrationRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "tools:manage");
        RequestSupport.requireEnvironment(servletRequest, request.environment());
        return Result.success(ToolDefinitionResponse.from(toolCatalogService.register(request.toCommand())));
    }

    @GetMapping
    public Result<List<ToolDefinitionResponse>> list(
            @RequestParam(defaultValue = "dev") String environment,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "tools:manage");
        RequestSupport.requireEnvironment(servletRequest, environment);
        return Result.success(toolCatalogService.listByEnvironment(environment).stream()
                .map(ToolDefinitionResponse::from)
                .toList());
    }
}
