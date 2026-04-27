package com.mcpgateway.app.integration.bigmarket;

import com.mcpgateway.app.integration.bigmarket.dto.BigMarketActivityRequest;
import com.mcpgateway.app.integration.bigmarket.dto.BigMarketOperationResponse;
import com.mcpgateway.app.integration.bigmarket.dto.BigMarketStrategyRequest;
import com.mcpgateway.app.integration.bigmarket.dto.BigMarketSystemOverviewResponse;
import com.mcpgateway.trigger.http.RequestSupport;
import com.mcpgateway.types.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/systems/big-market")
public class BigMarketConsoleController {

    private final BigMarketConsoleService bigMarketConsoleService;

    public BigMarketConsoleController(BigMarketConsoleService bigMarketConsoleService) {
        this.bigMarketConsoleService = bigMarketConsoleService;
    }

    @GetMapping
    public Result<BigMarketSystemOverviewResponse> overview(HttpServletRequest request) {
        RequestSupport.requireAnyScope(request, "system:big-market:read");
        RequestSupport.requireManagedSystem(request, "big-market-71772-z");
        return Result.success(bigMarketConsoleService.overview());
    }

    @PostMapping("/activity-armory")
    public Result<BigMarketOperationResponse> activityArmory(
            @RequestBody BigMarketActivityRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "system:big-market:operate");
        RequestSupport.requireManagedSystem(servletRequest, "big-market-71772-z");
        return Result.success(bigMarketConsoleService.activityArmory(request.activityId()));
    }

    @PostMapping("/strategy-armory")
    public Result<BigMarketOperationResponse> strategyArmory(
            @RequestBody BigMarketStrategyRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "system:big-market:operate");
        RequestSupport.requireManagedSystem(servletRequest, "big-market-71772-z");
        return Result.success(bigMarketConsoleService.strategyArmory(request));
    }

    @PostMapping("/draw")
    public Result<BigMarketOperationResponse> draw(
            @RequestBody BigMarketActivityRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "system:big-market:operate");
        RequestSupport.requireManagedSystem(servletRequest, "big-market-71772-z");
        return Result.success(bigMarketConsoleService.draw(request));
    }

    @PostMapping("/award-list")
    public Result<BigMarketOperationResponse> awardList(
            @RequestBody BigMarketActivityRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "system:big-market:read");
        RequestSupport.requireManagedSystem(servletRequest, "big-market-71772-z");
        return Result.success(bigMarketConsoleService.queryAwardList(request));
    }

    @PostMapping("/user-account")
    public Result<BigMarketOperationResponse> userAccount(
            @RequestBody BigMarketActivityRequest request,
            HttpServletRequest servletRequest
    ) {
        RequestSupport.requireAnyScope(servletRequest, "system:big-market:read");
        RequestSupport.requireManagedSystem(servletRequest, "big-market-71772-z");
        return Result.success(bigMarketConsoleService.queryUserActivityAccount(request));
    }
}
