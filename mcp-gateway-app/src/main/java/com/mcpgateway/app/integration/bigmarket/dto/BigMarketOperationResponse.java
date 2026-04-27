package com.mcpgateway.app.integration.bigmarket.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record BigMarketOperationResponse(
        String operation,
        String targetPath,
        boolean reachable,
        int statusCode,
        JsonNode payload,
        String message
) {
}

