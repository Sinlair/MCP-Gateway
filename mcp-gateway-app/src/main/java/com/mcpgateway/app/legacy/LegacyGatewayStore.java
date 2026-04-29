package com.mcpgateway.app.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class LegacyGatewayStore {

    private final ConcurrentMap<String, GatewayConfigRecord> gateways = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, GatewayToolRecord> tools = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, GatewayProtocolRecord> protocols = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, GatewayAuthRecord> auths = new ConcurrentHashMap<>();
    private final AtomicLong toolIdSequence = new AtomicLong(10000);
    private final AtomicLong protocolIdSequence = new AtomicLong(20000);
    private final ObjectMapper objectMapper;

    public LegacyGatewayStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        seedDefaults();
    }

    public List<GatewayConfigRecord> gateways() {
        return gateways.values().stream()
                .sorted(Comparator.comparing(GatewayConfigRecord::gatewayId))
                .toList();
    }

    public GatewayConfigRecord saveGateway(GatewayConfigRecord record) {
        gateways.put(record.gatewayId(), record);
        auths.computeIfAbsent(record.gatewayId(), gatewayId -> new GatewayAuthRecord(
                gatewayId,
                generateApiKey(),
                100,
                Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli()
        ));
        return record;
    }

    public List<GatewayToolRecord> tools() {
        return tools.values().stream()
                .sorted(Comparator.comparing(GatewayToolRecord::gatewayId).thenComparing(GatewayToolRecord::toolId))
                .toList();
    }

    public List<GatewayToolRecord> toolsByGatewayId(String gatewayId) {
        return tools().stream()
                .filter(tool -> Objects.equals(tool.gatewayId(), gatewayId))
                .toList();
    }

    public GatewayToolRecord saveTool(GatewayToolRecord record) {
        long toolId = record.toolId() == null ? toolIdSequence.incrementAndGet() : record.toolId();
        GatewayToolRecord saved = new GatewayToolRecord(
                record.gatewayId(),
                toolId,
                record.toolName(),
                record.toolType(),
                record.toolDescription(),
                record.toolVersion(),
                record.protocolId(),
                record.protocolType()
        );
        tools.put(toolId, saved);
        return saved;
    }

    public void deleteTool(Long toolId) {
        if (toolId != null) {
            tools.remove(toolId);
        }
    }

    public List<GatewayProtocolRecord> protocols() {
        return protocols.values().stream()
                .sorted(Comparator.comparing(GatewayProtocolRecord::protocolId))
                .toList();
    }

    public List<GatewayProtocolRecord> protocolsByGatewayId(String gatewayId) {
        List<Long> protocolIds = toolsByGatewayId(gatewayId).stream()
                .map(GatewayToolRecord::protocolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return protocols().stream()
                .filter(protocol -> protocolIds.contains(protocol.protocolId()))
                .toList();
    }

    public List<GatewayProtocolRecord> saveProtocols(List<GatewayProtocolRecord> items) {
        List<GatewayProtocolRecord> saved = new ArrayList<>();
        for (GatewayProtocolRecord record : items) {
            long protocolId = record.protocolId() == null ? protocolIdSequence.incrementAndGet() : record.protocolId();
            GatewayProtocolRecord protocol = new GatewayProtocolRecord(
                    protocolId,
                    record.httpUrl(),
                    record.httpMethod(),
                    record.httpHeaders(),
                    record.timeout(),
                    record.mappings()
            );
            protocols.put(protocolId, protocol);
            saved.add(protocol);
        }
        return saved;
    }

    public void deleteProtocol(Long protocolId) {
        if (protocolId == null) {
            return;
        }
        protocols.remove(protocolId);
        tools.values().stream()
                .filter(tool -> Objects.equals(tool.protocolId(), protocolId))
                .map(GatewayToolRecord::toolId)
                .toList()
                .forEach(tools::remove);
    }

    public List<GatewayAuthRecord> auths() {
        return auths.values().stream()
                .sorted(Comparator.comparing(GatewayAuthRecord::gatewayId))
                .toList();
    }

    public List<GatewayAuthRecord> authsByGatewayId(String gatewayId) {
        return auths().stream()
                .filter(auth -> Objects.equals(auth.gatewayId(), gatewayId))
                .toList();
    }

    public GatewayAuthRecord saveAuth(GatewayAuthRecord record) {
        GatewayAuthRecord existing = auths.get(record.gatewayId());
        GatewayAuthRecord saved = new GatewayAuthRecord(
                record.gatewayId(),
                existing != null ? existing.apiKey() : generateApiKey(),
                record.rateLimit(),
                record.expireTime()
        );
        auths.put(record.gatewayId(), saved);
        return saved;
    }

    public void deleteAuth(String gatewayId) {
        if (gatewayId != null) {
            auths.remove(gatewayId);
        }
    }

    public GatewayAuthRecord findAuth(String gatewayId) {
        return auths.get(gatewayId);
    }

    public GatewayConfigRecord findGateway(String gatewayId) {
        return gateways.get(gatewayId);
    }

    public List<GatewayProtocolRecord> analyzeProtocols(String openApiJson, List<String> endpoints) {
        if (openApiJson == null || openApiJson.isBlank() || endpoints == null || endpoints.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(openApiJson);
            String baseUrl = root.path("servers").path(0).path("url").asText("");
            JsonNode paths = root.path("paths");
            List<GatewayProtocolRecord> results = new ArrayList<>();
            for (String endpoint : endpoints) {
                JsonNode pathNode = paths.path(endpoint);
                if (pathNode.isMissingNode()) {
                    continue;
                }
                String method = detectMethod(pathNode);
                List<Map<String, Object>> mappings = new ArrayList<>();
                JsonNode parameters = pathNode.path(method).path("parameters");
                if (parameters.isArray()) {
                    for (JsonNode parameter : parameters) {
                        Map<String, Object> mapping = new LinkedHashMap<>();
                        mapping.put("mappingType", parameter.path("in").asText("query"));
                        mapping.put("fieldName", parameter.path("name").asText(""));
                        mapping.put("mcpPath", "$." + parameter.path("name").asText(""));
                        mapping.put("mcpType", parameter.path("schema").path("type").asText("string"));
                        mapping.put("mcpDesc", parameter.path("description").asText(""));
                        mapping.put("isRequired", parameter.path("required").asBoolean(false) ? 1 : 0);
                        mapping.put("sortOrder", mappings.size() + 1);
                        mappings.add(mapping);
                    }
                }
                results.add(new GatewayProtocolRecord(
                        null,
                        baseUrl + endpoint,
                        method,
                        "{\"Content-Type\":\"application/json\"}",
                        30000,
                        mappings
                ));
            }
            return results;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String detectMethod(JsonNode pathNode) {
        if (pathNode.has("post")) {
            return "post";
        }
        if (pathNode.has("get")) {
            return "get";
        }
        if (pathNode.has("put")) {
            return "put";
        }
        if (pathNode.has("delete")) {
            return "delete";
        }
        return "post";
    }

    private void seedDefaults() {
        saveGateway(new GatewayConfigRecord(
                "gateway_001",
                "MCP Gateway",
                "Default gateway workspace",
                "1.0.0",
                1,
                1
        ));
        saveProtocols(List.of(new GatewayProtocolRecord(
                1001L,
                "https://weather.example.com/mcp",
                "post",
                "{\"Content-Type\":\"application/json\"}",
                3000,
                List.of()
        )));
        saveTool(new GatewayToolRecord(
                "gateway_001",
                5001L,
                "forecast",
                "function",
                "Return mock forecast",
                "1.0.0",
                1001L,
                "http"
        ));
    }

    private String generateApiKey() {
        return "gw-" + UUID.randomUUID().toString().replace("-", "");
    }

    public record GatewayConfigRecord(
            String gatewayId,
            String gatewayName,
            String gatewayDesc,
            String version,
            Integer auth,
            Integer status
    ) {
    }

    public record GatewayToolRecord(
            String gatewayId,
            Long toolId,
            String toolName,
            String toolType,
            String toolDescription,
            String toolVersion,
            Long protocolId,
            String protocolType
    ) {
    }

    public record GatewayProtocolRecord(
            Long protocolId,
            String httpUrl,
            String httpMethod,
            String httpHeaders,
            Integer timeout,
            List<Map<String, Object>> mappings
    ) {
    }

    public record GatewayAuthRecord(
            String gatewayId,
            String apiKey,
            Integer rateLimit,
            Long expireTime
    ) {
    }
}
