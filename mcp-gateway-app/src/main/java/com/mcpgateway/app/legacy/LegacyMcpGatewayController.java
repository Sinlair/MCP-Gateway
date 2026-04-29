package com.mcpgateway.app.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api-gateway")
public class LegacyMcpGatewayController {

    private final LegacyMcpGatewayService gatewayService;
    private final LegacyMcpSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public LegacyMcpGatewayController(
            LegacyMcpGatewayService gatewayService,
            LegacyMcpSessionRegistry sessionRegistry,
            ObjectMapper objectMapper
    ) {
        this.gatewayService = gatewayService;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/{gatewayId}/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openSession(
            @PathVariable String gatewayId,
            @RequestParam(value = "api_key", required = false) String apiKey
    ) throws IOException {
        gatewayService.requireAccess(gatewayId, apiKey);
        LegacyMcpSession session = sessionRegistry.create(gatewayId, apiKey);
        sessionRegistry.sendEndpoint(session, "/api-gateway/" + gatewayId + "/mcp/sse?sessionId=" + session.sessionId() + (apiKey == null ? "" : "&api_key=" + apiKey));
        return session.emitter();
    }

    @PostMapping(value = "/{gatewayId}/mcp/sse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleMessage(
            @PathVariable String gatewayId,
            @RequestParam String sessionId,
            @RequestParam(value = "api_key", required = false) String apiKey,
            @RequestBody String messageBody
    ) throws IOException, InterruptedException {
        gatewayService.requireAccess(gatewayId, apiKey);
        LegacyMcpSession session = sessionRegistry.find(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        JsonNode payload = objectMapper.readTree(messageBody);
        String method = payload.path("method").asText("");
        JsonNode idNode = payload.get("id");
        Map<String, Object> result = switch (method) {
            case "initialize" -> gatewayService.initialize(gatewayId);
            case "tools/list" -> gatewayService.toolsList(gatewayId);
            case "tools/call" -> gatewayService.toolsCall(
                    gatewayId,
                    payload.path("params").path("name").asText(),
                    payload.path("params").path("arguments")
            );
            case "notifications/initialized", "ping" -> null;
            default -> throw new IllegalArgumentException("method not supported: " + method);
        };
        if (result != null) {
            sessionRegistry.sendMessage(session, Map.of(
                    "jsonrpc", "2.0",
                    "id", idNode,
                    "result", result
            ));
        }
        return ResponseEntity.accepted().build();
    }
}
