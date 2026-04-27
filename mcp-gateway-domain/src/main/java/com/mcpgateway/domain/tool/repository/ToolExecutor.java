package com.mcpgateway.domain.tool.repository;

import com.mcpgateway.domain.gateway.model.ToolExecutionResult;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.upstream.model.UpstreamServer;
import java.util.Map;

public interface ToolExecutor {

    ToolExecutionResult execute(
            GatewayClient client,
            UpstreamServer upstreamServer,
            ToolDefinition toolDefinition,
            Map<String, Object> arguments,
            String requestId,
            String sessionId
    );
}

