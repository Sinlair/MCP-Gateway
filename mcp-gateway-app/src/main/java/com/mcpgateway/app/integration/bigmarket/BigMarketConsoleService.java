package com.mcpgateway.app.integration.bigmarket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpgateway.app.integration.bigmarket.dto.BigMarketActivityRequest;
import com.mcpgateway.app.integration.bigmarket.dto.BigMarketOperationResponse;
import com.mcpgateway.app.integration.bigmarket.dto.BigMarketStrategyRequest;
import com.mcpgateway.app.integration.bigmarket.dto.BigMarketSystemOverviewResponse;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BigMarketConsoleService {

    private final BigMarketProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public BigMarketConsoleService(BigMarketProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public BigMarketSystemOverviewResponse overview() {
        return new BigMarketSystemOverviewResponse(
                properties.getName(),
                properties.getRepoPath(),
                properties.getBaseUrl(),
                properties.getApiVersion(),
                isReachable(),
                List.of(
                        "活动装配",
                        "策略装配",
                        "查询奖品列表",
                        "查询活动账户",
                        "执行抽奖"
                )
        );
    }

    public BigMarketOperationResponse activityArmory(Long activityId) {
        requireNonNull(activityId, "activityId");
        return executeGet(
                "活动装配",
                buildApiPath("/raffle/activity/armory?activityId=" + encode(activityId))
        );
    }

    public BigMarketOperationResponse strategyArmory(BigMarketStrategyRequest request) {
        requireNonNull(request.strategyId(), "strategyId");
        return executeGet(
                "策略装配",
                buildApiPath("/raffle/strategy/strategy_armory?strategyId=" + encode(request.strategyId()))
        );
    }

    public BigMarketOperationResponse draw(BigMarketActivityRequest request) {
        validateActivityRequest(request);
        return executePost("执行抽奖", buildApiPath("/raffle/activity/draw"), request);
    }

    public BigMarketOperationResponse queryAwardList(BigMarketActivityRequest request) {
        validateActivityRequest(request);
        return executePost(
                "查询奖品列表",
                buildApiPath("/raffle/strategy/query_raffle_award_list"),
                request
        );
    }

    public BigMarketOperationResponse queryUserActivityAccount(BigMarketActivityRequest request) {
        validateActivityRequest(request);
        return executePost(
                "查询活动账户",
                buildApiPath("/raffle/activity/query_user_activity_account"),
                request
        );
    }

    public boolean isReachable() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl()))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 500;
        } catch (Exception ignored) {
            return false;
        }
    }

    private BigMarketOperationResponse executeGet(String operation, String targetPath) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + targetPath))
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();
        return execute(operation, targetPath, request);
    }

    private BigMarketOperationResponse executePost(String operation, String targetPath, Object requestBody) {
        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + targetPath))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return execute(operation, targetPath, request);
        } catch (IOException e) {
            throw new AppException(ResponseCode.SYSTEM_ERROR, "failed to serialize request body");
        }
    }

    private BigMarketOperationResponse execute(String operation, String targetPath, HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode payload = parsePayload(response.body());
            return new BigMarketOperationResponse(
                    operation,
                    targetPath,
                    true,
                    response.statusCode(),
                    payload,
                    response.statusCode() < 400 ? "执行成功" : "执行返回异常状态"
            );
        } catch (ConnectException e) {
            throw new AppException(ResponseCode.CONFLICT, "big-market 系统当前不可达，请先启动 8091 服务");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AppException(ResponseCode.SYSTEM_ERROR, "调用 big-market 系统失败: " + e.getMessage());
        }
    }

    private JsonNode parsePayload(String body) {
        if (body == null || body.isBlank()) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            objectNode.put("message", "empty response body");
            return objectNode;
        }
        try {
            return objectMapper.readTree(body);
        } catch (IOException ignored) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            objectNode.put("raw", body);
            return objectNode;
        }
    }

    private String buildApiPath(String suffix) {
        return "/api/" + properties.getApiVersion() + suffix;
    }

    private void validateActivityRequest(BigMarketActivityRequest request) {
        if (request == null || request.activityId() == null || request.userId() == null || request.userId().isBlank()) {
            throw new AppException(ResponseCode.BAD_REQUEST, "userId 和 activityId 不能为空");
        }
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new AppException(ResponseCode.BAD_REQUEST, fieldName + " 不能为空");
        }
    }

    private String encode(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }
}
