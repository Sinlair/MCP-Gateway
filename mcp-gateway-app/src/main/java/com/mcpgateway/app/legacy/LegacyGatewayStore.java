package com.mcpgateway.app.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpgateway.app.legacy.persistence.McpGatewayAuthDao;
import com.mcpgateway.app.legacy.persistence.McpGatewayAuthPO;
import com.mcpgateway.app.legacy.persistence.McpGatewayDao;
import com.mcpgateway.app.legacy.persistence.McpGatewayPO;
import com.mcpgateway.app.legacy.persistence.McpGatewayToolDao;
import com.mcpgateway.app.legacy.persistence.McpGatewayToolPO;
import com.mcpgateway.app.legacy.persistence.McpProtocolHttpDao;
import com.mcpgateway.app.legacy.persistence.McpProtocolHttpPO;
import com.mcpgateway.app.legacy.persistence.McpProtocolMappingDao;
import com.mcpgateway.app.legacy.persistence.McpProtocolMappingPO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LegacyGatewayStore {

    private final McpGatewayDao gatewayDao;
    private final McpGatewayToolDao toolDao;
    private final McpProtocolHttpDao protocolHttpDao;
    private final McpProtocolMappingDao protocolMappingDao;
    private final McpGatewayAuthDao authDao;
    private final ObjectMapper objectMapper;

    public LegacyGatewayStore(
            McpGatewayDao gatewayDao,
            McpGatewayToolDao toolDao,
            McpProtocolHttpDao protocolHttpDao,
            McpProtocolMappingDao protocolMappingDao,
            McpGatewayAuthDao authDao,
            ObjectMapper objectMapper
    ) {
        this.gatewayDao = gatewayDao;
        this.toolDao = toolDao;
        this.protocolHttpDao = protocolHttpDao;
        this.protocolMappingDao = protocolMappingDao;
        this.authDao = authDao;
        this.objectMapper = objectMapper;
    }

    public List<GatewayConfigRecord> gateways() {
        return gatewayDao.queryAll().stream()
                .map(this::toGatewayRecord)
                .sorted(Comparator.comparing(GatewayConfigRecord::gatewayId))
                .toList();
    }

    public GatewayConfigRecord saveGateway(GatewayConfigRecord record) {
        McpGatewayPO po = new McpGatewayPO();
        po.setGatewayId(record.gatewayId());
        po.setGatewayName(record.gatewayName());
        po.setGatewayDesc(record.gatewayDesc());
        po.setVersion(record.version());
        po.setAuth(record.auth());
        po.setStatus(record.status());
        gatewayDao.insert(po);

        McpGatewayAuthPO auth = authDao.queryMcpGatewayAuthPO(gatewayIdQuery(record.gatewayId(), null));
        if (auth == null) {
            McpGatewayAuthPO authPO = new McpGatewayAuthPO();
            authPO.setGatewayId(record.gatewayId());
            authPO.setApiKey(generateApiKey());
            authPO.setRateLimit(100);
            authPO.setExpireTime(LocalDateTime.ofInstant(Instant.now().plusSeconds(30L * 24 * 3600), ZoneOffset.UTC));
            authPO.setStatus(1);
            authDao.insert(authPO);
        }
        return toGatewayRecord(gatewayDao.queryMcpGatewayByGatewayId(record.gatewayId()));
    }

    public List<GatewayToolRecord> tools() {
        return toolDao.queryAll().stream()
                .map(this::toGatewayToolRecord)
                .sorted(Comparator.comparing(GatewayToolRecord::gatewayId).thenComparing(GatewayToolRecord::toolId))
                .toList();
    }

    public List<GatewayToolRecord> toolsByGatewayId(String gatewayId) {
        return toolDao.queryListByGatewayId(gatewayId).stream()
                .map(this::toGatewayToolRecord)
                .toList();
    }

    public GatewayToolRecord saveTool(GatewayToolRecord record) {
        McpGatewayToolPO po = new McpGatewayToolPO();
        po.setGatewayId(record.gatewayId());
        po.setToolId(record.toolId() == null ? generateToolId() : record.toolId());
        po.setToolName(record.toolName());
        po.setToolType(record.toolType());
        po.setToolDescription(record.toolDescription());
        po.setToolVersion(record.toolVersion());
        po.setProtocolId(record.protocolId());
        po.setProtocolType(record.protocolType());
        toolDao.insert(po);
        return toGatewayToolRecord(po);
    }

    public void deleteTool(Long toolId) {
        toolDao.deleteByToolId(toolId);
    }

    public List<GatewayProtocolRecord> protocols() {
        return protocolHttpDao.queryAll().stream()
                .map(this::toProtocolRecord)
                .sorted(Comparator.comparing(GatewayProtocolRecord::protocolId))
                .toList();
    }

    public List<GatewayProtocolRecord> protocolsByGatewayId(String gatewayId) {
        List<Long> protocolIds = toolsByGatewayId(gatewayId).stream()
                .map(GatewayToolRecord::protocolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (protocolIds.isEmpty()) {
            return List.of();
        }
        return protocolHttpDao.queryListByProtocolIds(protocolIds).stream()
                .map(this::toProtocolRecord)
                .toList();
    }

    public List<GatewayProtocolRecord> saveProtocols(List<GatewayProtocolRecord> items) {
        List<GatewayProtocolRecord> saved = new ArrayList<>();
        for (GatewayProtocolRecord record : items) {
            McpProtocolHttpPO po = new McpProtocolHttpPO();
            Long protocolId = record.protocolId() == null ? generateProtocolId() : record.protocolId();
            po.setProtocolId(protocolId);
            po.setHttpUrl(record.httpUrl());
            po.setHttpMethod(record.httpMethod());
            po.setHttpHeaders(record.httpHeaders());
            po.setTimeout(record.timeout());
            po.setRetryTimes(3);
            po.setStatus(1);
            protocolHttpDao.insert(po);

            protocolMappingDao.deleteByProtocolId(protocolId);
            for (Map<String, Object> mapping : record.mappings()) {
                McpProtocolMappingPO mappingPO = new McpProtocolMappingPO();
                mappingPO.setProtocolId(protocolId);
                mappingPO.setMappingType(stringValue(mapping.get("mappingType")));
                mappingPO.setParentPath(stringValue(mapping.get("parentPath")));
                mappingPO.setFieldName(stringValue(mapping.get("fieldName")));
                mappingPO.setMcpPath(stringValue(mapping.get("mcpPath")));
                mappingPO.setMcpType(stringValue(mapping.get("mcpType")));
                mappingPO.setMcpDesc(stringValue(mapping.get("mcpDesc")));
                mappingPO.setIsRequired(intValue(mapping.get("isRequired"), 0));
                mappingPO.setSortOrder(intValue(mapping.get("sortOrder"), 0));
                protocolMappingDao.insert(mappingPO);
            }
            saved.add(toProtocolRecord(protocolHttpDao.queryMcpProtocolHttpByProtocolId(protocolId)));
        }
        return saved;
    }

    public void deleteProtocol(Long protocolId) {
        protocolMappingDao.deleteByProtocolId(protocolId);
        protocolHttpDao.deleteByProtocolId(protocolId);
    }

    public List<GatewayAuthRecord> auths() {
        return authDao.queryAll().stream()
                .map(this::toGatewayAuthRecord)
                .sorted(Comparator.comparing(GatewayAuthRecord::gatewayId))
                .toList();
    }

    public List<GatewayAuthRecord> authsByGatewayId(String gatewayId) {
        return authDao.queryListByGatewayId(gatewayId).stream()
                .map(this::toGatewayAuthRecord)
                .toList();
    }

    public GatewayAuthRecord saveAuth(GatewayAuthRecord record) {
        McpGatewayAuthPO existing = authDao.queryMcpGatewayAuthPO(gatewayIdQuery(record.gatewayId(), null));
        if (existing == null) {
            McpGatewayAuthPO created = new McpGatewayAuthPO();
            created.setGatewayId(record.gatewayId());
            created.setApiKey(generateApiKey());
            created.setRateLimit(record.rateLimit());
            created.setExpireTime(toLocalDateTime(record.expireTime()));
            created.setStatus(1);
            authDao.insert(created);
            return toGatewayAuthRecord(authDao.queryMcpGatewayAuthPO(gatewayIdQuery(record.gatewayId(), null)));
        }
        McpGatewayAuthPO update = new McpGatewayAuthPO();
        update.setGatewayId(record.gatewayId());
        update.setRateLimit(record.rateLimit());
        update.setExpireTime(toLocalDateTime(record.expireTime()));
        authDao.updateByGatewayId(update);
        return toGatewayAuthRecord(authDao.queryMcpGatewayAuthPO(gatewayIdQuery(record.gatewayId(), null)));
    }

    public void deleteAuth(String gatewayId) {
        authDao.deleteByGatewayId(gatewayId);
    }

    public GatewayAuthRecord findAuth(String gatewayId) {
        McpGatewayAuthPO po = authDao.queryMcpGatewayAuthPO(gatewayIdQuery(gatewayId, null));
        return po == null ? null : toGatewayAuthRecord(po);
    }

    public GatewayConfigRecord findGateway(String gatewayId) {
        McpGatewayPO po = gatewayDao.queryMcpGatewayByGatewayId(gatewayId);
        return po == null ? null : toGatewayRecord(po);
    }

    public GatewayToolRecord findTool(String gatewayId, String toolName) {
        return toolsByGatewayId(gatewayId).stream()
                .filter(tool -> Objects.equals(tool.toolName(), toolName))
                .findFirst()
                .orElse(null);
    }

    public GatewayProtocolRecord findProtocol(Long protocolId) {
        if (protocolId == null) {
            return null;
        }
        McpProtocolHttpPO po = protocolHttpDao.queryMcpProtocolHttpByProtocolId(protocolId);
        return po == null ? null : toProtocolRecord(po);
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
                JsonNode operation = pathNode.path(method);
                JsonNode parameters = operation.path("parameters");
                if (parameters.isArray()) {
                    int sort = 1;
                    for (JsonNode parameter : parameters) {
                        Map<String, Object> mapping = new LinkedHashMap<>();
                        mapping.put("mappingType", parameter.path("in").asText("query"));
                        mapping.put("parentPath", null);
                        mapping.put("fieldName", parameter.path("name").asText(""));
                        mapping.put("mcpPath", parameter.path("name").asText(""));
                        mapping.put("mcpType", parameter.path("schema").path("type").asText("string"));
                        mapping.put("mcpDesc", parameter.path("description").asText(""));
                        mapping.put("isRequired", parameter.path("required").asBoolean(false) ? 1 : 0);
                        mapping.put("sortOrder", sort++);
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

    private McpGatewayAuthPO gatewayIdQuery(String gatewayId, String apiKey) {
        McpGatewayAuthPO query = new McpGatewayAuthPO();
        query.setGatewayId(gatewayId);
        query.setApiKey(apiKey);
        return query;
    }

    private GatewayConfigRecord toGatewayRecord(McpGatewayPO po) {
        return new GatewayConfigRecord(
                po.getGatewayId(),
                po.getGatewayName(),
                po.getGatewayDesc(),
                po.getVersion(),
                po.getAuth(),
                po.getStatus()
        );
    }

    private GatewayToolRecord toGatewayToolRecord(McpGatewayToolPO po) {
        return new GatewayToolRecord(
                po.getGatewayId(),
                po.getToolId(),
                po.getToolName(),
                po.getToolType(),
                po.getToolDescription(),
                po.getToolVersion(),
                po.getProtocolId(),
                po.getProtocolType()
        );
    }

    private GatewayProtocolRecord toProtocolRecord(McpProtocolHttpPO po) {
        List<Map<String, Object>> mappings = protocolMappingDao
                .queryMcpGatewayToolConfigListByProtocolId(po.getProtocolId()).stream()
                .map(mapping -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("mappingType", mapping.getMappingType());
                    item.put("parentPath", mapping.getParentPath());
                    item.put("fieldName", mapping.getFieldName());
                    item.put("mcpPath", mapping.getMcpPath());
                    item.put("mcpType", mapping.getMcpType());
                    item.put("mcpDesc", mapping.getMcpDesc());
                    item.put("isRequired", mapping.getIsRequired());
                    item.put("sortOrder", mapping.getSortOrder());
                    return item;
                })
                .toList();
        return new GatewayProtocolRecord(
                po.getProtocolId(),
                po.getHttpUrl(),
                po.getHttpMethod(),
                po.getHttpHeaders(),
                po.getTimeout(),
                mappings
        );
    }

    private GatewayAuthRecord toGatewayAuthRecord(McpGatewayAuthPO po) {
        return new GatewayAuthRecord(
                po.getGatewayId(),
                po.getApiKey(),
                po.getRateLimit(),
                po.getExpireTime() == null ? null : po.getExpireTime().toInstant(ZoneOffset.UTC).toEpochMilli()
        );
    }

    private LocalDateTime toLocalDateTime(Long epochMilli) {
        return epochMilli == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
    }

    private long generateToolId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000L);
    }

    private long generateProtocolId() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100_000_000L);
    }

    private String generateApiKey() {
        return "gw-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception exception) {
            return fallback;
        }
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
