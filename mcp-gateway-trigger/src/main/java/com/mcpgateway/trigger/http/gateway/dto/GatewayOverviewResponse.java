package com.mcpgateway.trigger.http.gateway.dto;

import com.mcpgateway.trigger.http.admin.dto.UpstreamServerResponse;
import java.util.List;

public record GatewayOverviewResponse(
        String environment,
        String callerId,
        int totalUpstreams,
        int enabledUpstreams,
        int routableUpstreams,
        int discoverableTools,
        List<String> boundedContexts,
        List<UpstreamServerResponse> enabledServers
) {
}
