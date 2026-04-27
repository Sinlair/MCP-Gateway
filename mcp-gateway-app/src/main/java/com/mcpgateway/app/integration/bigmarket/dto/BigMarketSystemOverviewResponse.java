package com.mcpgateway.app.integration.bigmarket.dto;

import java.util.List;

public record BigMarketSystemOverviewResponse(
        String systemName,
        String repoPath,
        String baseUrl,
        String apiVersion,
        boolean reachable,
        List<String> supportedOperations
) {
}

