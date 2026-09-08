package com.mcpgateway.infrastructure.tool.executor;

import com.mcpgateway.domain.gateway.model.ToolExecutionResult;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.tool.repository.ToolExecutor;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class MockToolExecutor implements ToolExecutor {

    private final String mode;
    private final int timeoutMs;
    private final int maxRetries;

    public MockToolExecutor(
            @Value("${mcp.gateway.tool-execution.mode:mock}") String mode,
            @Value("${mcp.gateway.tool-execution.timeout-ms:3000}") int timeoutMs,
            @Value("${mcp.gateway.tool-execution.max-retries:1}") int maxRetries
    ) {
        this.mode = mode;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
    }

    @Override
    public ToolExecutionResult execute(
            GatewayClient client,
            UpstreamServer upstreamServer,
            ToolDefinition toolDefinition,
            Map<String, Object> arguments,
            String requestId,
            String sessionId
    ) {
        if (!"mock".equalsIgnoreCase(mode)) {
            throw new AppException(
                    ResponseCode.CONFLICT,
                    "unsupported tool execution mode: " + mode
            );
        }
        List<Throwable> failures = new ArrayList<>();
        int attempts = Math.max(1, maxRetries + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return CompletableFuture.supplyAsync(
                                () -> buildMockResult(client, upstreamServer, toolDefinition, arguments, requestId, sessionId)
                        )
                        .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                        .join();
            } catch (Exception exception) {
                failures.add(exception);
                if (attempt == attempts) {
                    throw new AppException(
                            ResponseCode.SYSTEM_ERROR,
                            "tool execution failed after retries: " + summarizeFailures(failures)
                    );
                }
            }
        }
        throw new AppException(ResponseCode.SYSTEM_ERROR, "tool execution failed");
    }

    private ToolExecutionResult buildMockResult(
            GatewayClient client,
            UpstreamServer upstreamServer,
            ToolDefinition toolDefinition,
            Map<String, Object> arguments,
            String requestId,
            String sessionId
    ) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("mode", "mock");
        output.put("executedAt", Instant.now().toString());
        output.put("requestId", requestId);
        output.put("sessionId", sessionId);
        output.put("clientId", client.clientId());
        output.put("upstream", upstreamServer.serverCode());
        output.put("tool", toolDefinition.toolName());
        output.put("arguments", arguments == null ? Map.of() : arguments);
        output.put("summary", "Mock execution completed for " + toolDefinition.toolIdentifier());
        return new ToolExecutionResult("SUCCESS", output);
    }

    private String summarizeFailures(List<Throwable> failures) {
        return failures.stream()
                .map(Throwable::getMessage)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("unknown error");
    }
}
