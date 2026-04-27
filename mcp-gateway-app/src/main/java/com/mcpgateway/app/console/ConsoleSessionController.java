package com.mcpgateway.app.console;

import com.mcpgateway.app.console.dto.ConsoleAuditResponse;
import com.mcpgateway.app.console.dto.ConsoleSessionResponse;
import com.mcpgateway.app.console.dto.ConsoleTokenIssueRequest;
import com.mcpgateway.app.console.dto.ConsoleTokenResponse;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.types.context.RequestAttributeNames;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import com.mcpgateway.types.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class ConsoleSessionController {

    private final ConsoleTokenService consoleTokenService;

    public ConsoleSessionController(ConsoleTokenService consoleTokenService) {
        this.consoleTokenService = consoleTokenService;
    }

    @PostMapping("/api/v1/public/console/tokens/demo")
    public Result<ConsoleTokenResponse> issueDemoToken(@RequestBody ConsoleTokenIssueRequest request) {
        return Result.success(consoleTokenService.issueDemoToken(request));
    }

    @GetMapping("/api/v1/console/session")
    public Result<ConsoleSessionResponse> session(HttpServletRequest request) {
        Object token = request.getAttribute(RequestAttributeNames.CONSOLE_TOKEN_SESSION);
        if (token instanceof ConsoleTokenSession session) {
            return Result.success(consoleTokenService.introspect(session));
        }
        throw new AppException(ResponseCode.UNAUTHORIZED, "当前访问令牌不是控制台令牌");
    }

    @PostMapping("/api/v1/console/tokens/revoke")
    public Result<Boolean> revoke(HttpServletRequest request) {
        Object token = request.getAttribute(RequestAttributeNames.CONSOLE_TOKEN_SESSION);
        if (token instanceof ConsoleTokenSession session) {
            consoleTokenService.revoke(session, "控制台主动退出，访问令牌已撤销");
            return Result.success(true);
        }
        throw new AppException(ResponseCode.UNAUTHORIZED, "当前访问令牌不是控制台令牌");
    }

    @PostMapping("/api/v1/admin/console/tokens")
    public Result<ConsoleTokenResponse> issueAdminToken(
            @RequestBody ConsoleTokenIssueRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "console:admin");
        return Result.success(consoleTokenService.issueDemoToken(request));
    }

    @GetMapping("/api/v1/admin/console/audits")
    public Result<List<ConsoleAuditResponse>> audits(
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "console:admin");
        return Result.success(consoleTokenService.listAudits(limit));
    }
}
