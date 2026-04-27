package com.mcpgateway.infrastructure.tool.executor;

import com.mcpgateway.domain.gateway.model.ToolExecutionResult;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.tool.repository.ToolExecutor;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MockToolExecutor implements ToolExecutor {

    @Override
    public ToolExecutionResult execute(
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
}
