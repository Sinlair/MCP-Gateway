package com.mcpgateway.app.legacy;

import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayAuthRecord;
import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayConfigRecord;
import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayProtocolRecord;
import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayToolRecord;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-gateway/admin")
public class LegacyAdminController {

    private final LegacyGatewayStore store;
    private final LegacyMcpGatewayService legacyMcpGatewayService;

    public LegacyAdminController(LegacyGatewayStore store, LegacyMcpGatewayService legacyMcpGatewayService) {
        this.store = store;
        this.legacyMcpGatewayService = legacyMcpGatewayService;
    }

    @PostMapping("/save_gateway_config")
    public ResponseEntity<Map<String, Object>> saveGatewayConfig(@RequestBody Map<String, Object> body) {
        GatewayConfigRecord saved = store.saveGateway(new GatewayConfigRecord(
                stringValue(body.get("gatewayId")),
                stringValue(body.get("gatewayName")),
                stringValue(body.get("gatewayDesc")),
                stringValue(body.get("version")),
                intValue(body.get("auth"), 1),
                intValue(body.get("status"), 1)
        ));
        return ResponseEntity.ok(success(toGatewayConfigDto(saved)));
    }

    @PostMapping("/save_gateway_tool_config")
    public ResponseEntity<Map<String, Object>> saveGatewayToolConfig(@RequestBody Map<String, Object> body) {
        GatewayToolRecord saved = store.saveTool(new GatewayToolRecord(
                stringValue(body.get("gatewayId")),
                longValue(body.get("toolId")),
                stringValue(body.get("toolName")),
                stringValue(body.get("toolType")),
                stringValue(body.get("toolDescription")),
                stringValue(body.get("toolVersion")),
                longValue(body.get("protocolId")),
                stringValue(body.get("protocolType"))
        ));
        return ResponseEntity.ok(success(toGatewayToolDto(saved)));
    }

    @PostMapping("/save_gateway_protocol")
    public ResponseEntity<Map<String, Object>> saveGatewayProtocol(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> httpProtocols = listOfMaps(body.get("httpProtocols"));
        List<GatewayProtocolRecord> saved = store.saveProtocols(httpProtocols.stream()
                .map(protocol -> new GatewayProtocolRecord(
                        longValue(protocol.get("protocolId")),
                        stringValue(protocol.get("httpUrl")),
                        stringValue(protocol.get("httpMethod")),
                        stringValue(protocol.get("httpHeaders")),
                        intValue(protocol.get("timeout"), 5000),
                        listOfMaps(protocol.get("mappings"))
                ))
                .toList());
        return ResponseEntity.ok(success(Map.of("success", !saved.isEmpty())));
    }

    @PostMapping("/import_gateway_protocol")
    public ResponseEntity<Map<String, Object>> importGatewayProtocol(@RequestBody Map<String, Object> body) {
        List<GatewayProtocolRecord> analyzed = store.analyzeProtocols(
                stringValue(body.get("openApiJson")),
                stringList(body.get("endpoints"))
        );
        store.saveProtocols(analyzed);
        return ResponseEntity.ok(success(Map.of("success", true)));
    }

    @PostMapping("/analysis_protocol")
    public ResponseEntity<Map<String, Object>> analysisProtocol(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> result = store.analyzeProtocols(
                        stringValue(body.get("openApiJson")),
                        stringList(body.get("endpoints"))
                ).stream()
                .map(this::toGatewayProtocolDto)
                .toList();
        return ResponseEntity.ok(success(result));
    }

    @PostMapping("/save_gateway_auth")
    public ResponseEntity<Map<String, Object>> saveGatewayAuth(@RequestBody Map<String, Object> body) {
        GatewayAuthRecord saved = store.saveAuth(new GatewayAuthRecord(
                stringValue(body.get("gatewayId")),
                null,
                intValue(body.get("rateLimit"), 100),
                longValue(body.get("expireTime")) != null
                        ? longValue(body.get("expireTime"))
                        : Instant.now().plusSeconds(30L * 24 * 3600).toEpochMilli()
        ));
        return ResponseEntity.ok(success(toGatewayAuthDto(saved)));
    }

    @GetMapping("/query_gateway_config_list")
    public ResponseEntity<Map<String, Object>> queryGatewayConfigList() {
        return ResponseEntity.ok(success(store.gateways().stream().map(this::toGatewayConfigDto).toList()));
    }

    @GetMapping("/query_gateway_config_page")
    public ResponseEntity<Map<String, Object>> queryGatewayConfigPage(
            @RequestParam(required = false) String gatewayId,
            @RequestParam(required = false) String gatewayName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int rows
    ) {
        List<Map<String, Object>> items = store.gateways().stream()
                .filter(item -> gatewayId == null || gatewayId.isBlank() || item.gatewayId().contains(gatewayId))
                .filter(item -> gatewayName == null || gatewayName.isBlank() || item.gatewayName().contains(gatewayName))
                .map(this::toGatewayConfigDto)
                .toList();
        return ResponseEntity.ok(successPage(paginate(items, page, rows), items.size()));
    }

    @GetMapping("/query_gateway_tool_list")
    public ResponseEntity<Map<String, Object>> queryGatewayToolList() {
        return ResponseEntity.ok(success(store.tools().stream().map(this::toGatewayToolDto).toList()));
    }

    @GetMapping("/query_gateway_tool_page")
    public ResponseEntity<Map<String, Object>> queryGatewayToolPage(
            @RequestParam(required = false) String gatewayId,
            @RequestParam(required = false) Long toolId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int rows
    ) {
        List<Map<String, Object>> items = store.tools().stream()
                .filter(item -> gatewayId == null || gatewayId.isBlank() || item.gatewayId().contains(gatewayId))
                .filter(item -> toolId == null || Objects.equals(item.toolId(), toolId))
                .map(this::toGatewayToolDto)
                .toList();
        return ResponseEntity.ok(successPage(paginate(items, page, rows), items.size()));
    }

    @GetMapping("/query_gateway_tool_list_by_gateway_id")
    public ResponseEntity<Map<String, Object>> queryGatewayToolListByGatewayId(@RequestParam String gatewayId) {
        return ResponseEntity.ok(success(store.toolsByGatewayId(gatewayId).stream().map(this::toGatewayToolDto).toList()));
    }

    @PostMapping("/delete_gateway_tool_config")
    public ResponseEntity<Map<String, Object>> deleteGatewayToolConfig(@RequestParam Long toolId) {
        store.deleteTool(toolId);
        return ResponseEntity.ok(success(Map.of("success", true)));
    }

    @GetMapping("/query_gateway_protocol_list")
    public ResponseEntity<Map<String, Object>> queryGatewayProtocolList() {
        return ResponseEntity.ok(success(store.protocols().stream().map(this::toGatewayProtocolDto).toList()));
    }

    @GetMapping("/query_gateway_protocol_page")
    public ResponseEntity<Map<String, Object>> queryGatewayProtocolPage(
            @RequestParam(required = false) Long protocolId,
            @RequestParam(required = false) String httpUrl,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int rows
    ) {
        List<Map<String, Object>> items = store.protocols().stream()
                .filter(item -> protocolId == null || Objects.equals(item.protocolId(), protocolId))
                .filter(item -> httpUrl == null || httpUrl.isBlank() || item.httpUrl().contains(httpUrl))
                .sorted(Comparator.comparing(GatewayProtocolRecord::protocolId))
                .map(this::toGatewayProtocolDto)
                .toList();
        return ResponseEntity.ok(successPage(paginate(items, page, rows), items.size()));
    }

    @GetMapping("/query_gateway_protocol_list_by_gateway_id")
    public ResponseEntity<Map<String, Object>> queryGatewayProtocolListByGatewayId(@RequestParam String gatewayId) {
        return ResponseEntity.ok(success(store.protocolsByGatewayId(gatewayId).stream().map(this::toGatewayProtocolDto).toList()));
    }

    @PostMapping("/delete_gateway_protocol")
    public ResponseEntity<Map<String, Object>> deleteGatewayProtocol(@RequestParam Long protocolId) {
        store.deleteProtocol(protocolId);
        return ResponseEntity.ok(success(Map.of("success", true)));
    }

    @GetMapping("/query_gateway_auth_list")
    public ResponseEntity<Map<String, Object>> queryGatewayAuthList() {
        return ResponseEntity.ok(success(store.auths().stream().map(this::toGatewayAuthDto).toList()));
    }

    @GetMapping("/query_gateway_auth_page")
    public ResponseEntity<Map<String, Object>> queryGatewayAuthPage(
            @RequestParam(required = false) String gatewayId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int rows
    ) {
        List<Map<String, Object>> items = store.auths().stream()
                .filter(item -> gatewayId == null || gatewayId.isBlank() || item.gatewayId().contains(gatewayId))
                .map(this::toGatewayAuthDto)
                .toList();
        return ResponseEntity.ok(successPage(paginate(items, page, rows), items.size()));
    }

    @GetMapping("/query_gateway_auth_list_by_gateway_id")
    public ResponseEntity<Map<String, Object>> queryGatewayAuthListByGatewayId(@RequestParam String gatewayId) {
        return ResponseEntity.ok(success(store.authsByGatewayId(gatewayId).stream().map(this::toGatewayAuthDto).toList()));
    }

    @PostMapping("/delete_gateway_auth")
    public ResponseEntity<Map<String, Object>> deleteGatewayAuth(@RequestParam String gatewayId) {
        store.deleteAuth(gatewayId);
        return ResponseEntity.ok(success(Map.of("success", true)));
    }

    @PostMapping("/test_call_gateway")
    public ResponseEntity<Map<String, Object>> testCallGateway(@RequestBody Map<String, Object> body) {
        String gatewayId = stringValue(body.get("gatewayId"));
        String message = stringValue(body.get("message"));
        String authApiKey = stringValue(body.get("authApiKey"));
        GatewayConfigRecord gateway = store.findGateway(gatewayId);
        if (gateway == null) {
            return ResponseEntity.ok(failure("gateway not found"));
        }
        GatewayAuthRecord auth = store.findAuth(gatewayId);
        if (gateway.auth() != null && gateway.auth() == 1) {
            if (auth == null || authApiKey == null || !auth.apiKey().equals(authApiKey)) {
                return ResponseEntity.ok(failure("invalid auth api key"));
            }
            if (auth.expireTime() != null && auth.expireTime() < System.currentTimeMillis()) {
                return ResponseEntity.ok(failure("auth api key expired"));
            }
        }
        try {
            legacyMcpGatewayService.requireAccess(gatewayId, authApiKey);
            return ResponseEntity.ok(success(legacyMcpGatewayService.testGatewayCall(gatewayId, message)));
        } catch (Exception exception) {
            return ResponseEntity.ok(failure(exception.getMessage()));
        }
    }

    private Map<String, Object> toGatewayConfigDto(GatewayConfigRecord record) {
        return new LinkedHashMap<>(Map.of(
                "gatewayId", record.gatewayId(),
                "gatewayName", record.gatewayName(),
                "gatewayDesc", record.gatewayDesc() == null ? "" : record.gatewayDesc(),
                "version", record.version() == null ? "" : record.version(),
                "auth", record.auth() == null ? 0 : record.auth(),
                "status", record.status() == null ? 0 : record.status()
        ));
    }

    private Map<String, Object> toGatewayToolDto(GatewayToolRecord record) {
        return new LinkedHashMap<>(Map.of(
                "gatewayId", record.gatewayId(),
                "toolId", record.toolId(),
                "toolName", record.toolName() == null ? "" : record.toolName(),
                "toolType", record.toolType() == null ? "" : record.toolType(),
                "toolDescription", record.toolDescription() == null ? "" : record.toolDescription(),
                "toolVersion", record.toolVersion() == null ? "" : record.toolVersion(),
                "protocolId", record.protocolId(),
                "protocolType", record.protocolType() == null ? "" : record.protocolType()
        ));
    }

    private Map<String, Object> toGatewayProtocolDto(GatewayProtocolRecord record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("protocolId", record.protocolId());
        response.put("httpUrl", record.httpUrl());
        response.put("httpMethod", record.httpMethod());
        response.put("httpHeaders", record.httpHeaders());
        response.put("timeout", record.timeout());
        response.put("mappings", record.mappings());
        return response;
    }

    private Map<String, Object> toGatewayAuthDto(GatewayAuthRecord record) {
        return new LinkedHashMap<>(Map.of(
                "gatewayId", record.gatewayId(),
                "apiKey", record.apiKey(),
                "rateLimit", record.rateLimit() == null ? 0 : record.rateLimit(),
                "expireTime", record.expireTime()
        ));
    }

    private Map<String, Object> success(Object data) {
        return new LinkedHashMap<>(Map.of(
                "code", "0000",
                "info", "success",
                "data", data
        ));
    }

    private Map<String, Object> successPage(Object data, int total) {
        Map<String, Object> response = success(data);
        response.put("total", total);
        return response;
    }

    private Map<String, Object> failure(String info) {
        return new LinkedHashMap<>(Map.of(
                "code", "9999",
                "info", info
        ));
    }

    private List<Map<String, Object>> paginate(List<Map<String, Object>> items, int page, int rows) {
        int safePage = Math.max(page, 1);
        int safeRows = Math.max(rows, 1);
        int from = Math.min((safePage - 1) * safeRows, items.size());
        int to = Math.min(from + safeRows, items.size());
        return items.subList(from, to);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
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

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception exception) {
            return null;
        }
    }
}
