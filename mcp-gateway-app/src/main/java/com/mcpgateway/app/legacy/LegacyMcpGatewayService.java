package com.mcpgateway.app.legacy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayAuthRecord;
import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayConfigRecord;
import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayProtocolRecord;
import com.mcpgateway.app.legacy.LegacyGatewayStore.GatewayToolRecord;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LegacyMcpGatewayService {

    private final LegacyGatewayStore store;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public LegacyMcpGatewayService(LegacyGatewayStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public void requireAccess(String gatewayId, String apiKey) {
        GatewayConfigRecord gateway = store.findGateway(gatewayId);
        if (gateway == null) {
            throw new IllegalArgumentException("gatewayId not found");
        }
        if (gateway.status() != null && gateway.status() == 0) {
            throw new IllegalArgumentException("gateway is disabled");
        }
        if (gateway.auth() != null && gateway.auth() == 1) {
            GatewayAuthRecord auth = store.findAuth(gatewayId);
            if (auth == null || auth.apiKey() == null || !Objects.equals(auth.apiKey(), apiKey)) {
                throw new IllegalArgumentException("invalid api key");
            }
            if (auth.expireTime() != null && auth.expireTime() < System.currentTimeMillis()) {
                throw new IllegalArgumentException("api key expired");
            }
        }
    }

    public Map<String, Object> initialize(String gatewayId) {
        GatewayConfigRecord gateway = store.findGateway(gatewayId);
        return Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", true),
                        "resources", Map.of("listChanged", false, "subscribe", false),
                        "prompts", Map.of("listChanged", false)
                ),
                "serverInfo", Map.of(
                        "name", gateway.gatewayName(),
                        "version", gateway.version() == null ? "1.0.0" : gateway.version()
                ),
                "instructions", gateway.gatewayDesc() == null ? "" : gateway.gatewayDesc()
        );
    }

    public Map<String, Object> toolsList(String gatewayId) {
        List<Map<String, Object>> tools = store.toolsByGatewayId(gatewayId).stream()
                .map(tool -> Map.of(
                        "name", tool.toolName(),
                        "description", tool.toolDescription() == null ? "" : tool.toolDescription(),
                        "inputSchema", buildInputSchema(tool)
                ))
                .toList();
        return Map.of("tools", tools);
    }

    public Map<String, Object> toolsCall(String gatewayId, String toolName, JsonNode argumentsNode) throws IOException, InterruptedException {
        GatewayToolRecord tool = store.findTool(gatewayId, toolName);
        if (tool == null) {
            throw new IllegalArgumentException("tool not found");
        }
        GatewayProtocolRecord protocol = store.findProtocol(tool.protocolId());
        if (protocol == null) {
            throw new IllegalArgumentException("protocol not found");
        }
        Map<String, Object> arguments = objectMapper.convertValue(
                argumentsNode == null ? objectMapper.createObjectNode() : argumentsNode,
                new TypeReference<>() { }
        );

        String result = executeProtocol(protocol, arguments);
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", result)),
                "isError", false
        );
    }

    public Map<String, Object> testGatewayCall(String gatewayId, String message) throws IOException, InterruptedException {
        List<GatewayToolRecord> tools = store.toolsByGatewayId(gatewayId);
        if (tools.isEmpty()) {
            throw new IllegalArgumentException("no tools configured for gateway");
        }
        GatewayToolRecord tool = tools.get(0);
        GatewayProtocolRecord protocol = store.findProtocol(tool.protocolId());
        if (protocol == null) {
            throw new IllegalArgumentException("protocol not found");
        }
        Map<String, Object> arguments = deriveArguments(protocol, message);
        String content = executeProtocol(protocol, arguments);
        return Map.of(
                "gatewayId", gatewayId,
                "toolName", tool.toolName(),
                "arguments", arguments,
                "content", content,
                "answer", content
        );
    }

    private Map<String, Object> buildInputSchema(GatewayToolRecord tool) {
        GatewayProtocolRecord protocol = store.findProtocol(tool.protocolId());
        List<Map<String, Object>> mappings = protocol == null ? List.of() : protocol.mappings();
        Map<String, List<Map<String, Object>>> childrenMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> mapping : mappings) {
            String parentPath = stringValue(mapping.get("parentPath"));
            if (parentPath == null || parentPath.isBlank()) {
                roots.add(mapping);
            } else {
                childrenMap.computeIfAbsent(parentPath, key -> new ArrayList<>()).add(mapping);
            }
        }

        roots.sort((left, right) -> Integer.compare(
                intValue(left.get("sortOrder"), 0),
                intValue(right.get("sortOrder"), 0)
        ));

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Map<String, Object> root : roots) {
            String fieldName = stringValue(root.get("fieldName"));
            properties.put(fieldName, buildProperty(root, childrenMap));
            if (intValue(root.get("isRequired"), 0) == 1) {
                required.add(fieldName);
            }
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private Map<String, Object> buildProperty(
            Map<String, Object> current,
            Map<String, List<Map<String, Object>>> childrenMap
    ) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", normalizedType(stringValue(current.get("mcpType"))));
        if (stringValue(current.get("mcpDesc")) != null) {
            property.put("description", stringValue(current.get("mcpDesc")));
        }
        String currentPath = stringValue(current.get("mcpPath"));
        List<Map<String, Object>> children = Optional.ofNullable(childrenMap.get(currentPath)).orElse(List.of());
        if (!children.isEmpty()) {
            children = new ArrayList<>(children);
            children.sort((left, right) -> Integer.compare(
                    intValue(left.get("sortOrder"), 0),
                    intValue(right.get("sortOrder"), 0)
            ));
            Map<String, Object> childProperties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (Map<String, Object> child : children) {
                String fieldName = stringValue(child.get("fieldName"));
                childProperties.put(fieldName, buildProperty(child, childrenMap));
                if (intValue(child.get("isRequired"), 0) == 1) {
                    required.add(fieldName);
                }
            }
            property.put("type", "object");
            property.put("properties", childProperties);
            property.put("required", required);
            property.put("additionalProperties", false);
        }
        return property;
    }

    private String executeProtocol(GatewayProtocolRecord protocol, Map<String, Object> arguments) throws IOException, InterruptedException {
        String method = protocol.httpMethod() == null ? "post" : protocol.httpMethod().toLowerCase();
        Map<String, String> headers = parseHeaders(protocol.httpHeaders());
        if ("get".equals(method)) {
            Map<String, Object> payload = flattenRequestPayload(arguments);
            String url = buildGetUrl(protocol.httpUrl(), payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
            headers.forEach(builder::header);
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
        }

        Object requestBody = flattenPostPayload(arguments);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(protocol.httpUrl()))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));
        headers.forEach(builder::header);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private Map<String, Object> deriveArguments(GatewayProtocolRecord protocol, String message) {
        if (message != null && message.trim().startsWith("{")) {
            try {
                return objectMapper.readValue(message, new TypeReference<>() { });
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        List<Map<String, Object>> mappings = protocol.mappings();
        if (mappings == null || mappings.isEmpty()) {
            arguments.put("message", message == null ? "" : message);
            return arguments;
        }
        List<Map<String, Object>> roots = mappings.stream()
                .filter(mapping -> stringValue(mapping.get("parentPath")) == null)
                .sorted(Comparator.comparingInt(mapping -> intValue(mapping.get("sortOrder"), 0)))
                .toList();
        if (roots.isEmpty()) {
            arguments.put("message", message == null ? "" : message);
            return arguments;
        }
        for (Map<String, Object> root : roots) {
            String fieldName = stringValue(root.get("fieldName"));
            arguments.put(fieldName, sampleValue(root, mappings, message));
        }
        return arguments;
    }

    private Object sampleValue(Map<String, Object> current, List<Map<String, Object>> mappings, String message) {
        List<Map<String, Object>> children = mappings.stream()
                .filter(mapping -> Objects.equals(stringValue(mapping.get("parentPath")), stringValue(current.get("mcpPath"))))
                .sorted(Comparator.comparingInt(mapping -> intValue(mapping.get("sortOrder"), 0)))
                .toList();
        if (!children.isEmpty()) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map<String, Object> child : children) {
                nested.put(stringValue(child.get("fieldName")), sampleValue(child, mappings, message));
            }
            return nested;
        }
        String type = normalizedType(stringValue(current.get("mcpType")));
        return switch (type) {
            case "integer", "number" -> 0;
            case "boolean" -> false;
            case "array" -> List.of();
            default -> message == null ? "" : message;
        };
    }

    @SuppressWarnings("unchecked")
    private Object flattenPostPayload(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        if (arguments.size() == 1) {
            return arguments.values().iterator().next();
        }
        return arguments;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenRequestPayload(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        if (arguments.size() == 1) {
            Object value = arguments.values().iterator().next();
            if (value instanceof Map<?, ?> mapValue) {
                return new LinkedHashMap<>((Map<String, Object>) mapValue);
            }
        }
        return new LinkedHashMap<>(arguments);
    }

    private String buildGetUrl(String rawUrl, Map<String, Object> payload) {
        String url = rawUrl;
        Map<String, Object> query = new LinkedHashMap<>(payload);
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String token = "{" + entry.getKey() + "}";
            if (url.contains(token)) {
                url = url.replace(token, String.valueOf(entry.getValue()));
                query.remove(entry.getKey());
            }
        }
        if (query.isEmpty()) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + query.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));
    }

    private Map<String, String> parseHeaders(String rawHeaders) {
        if (rawHeaders == null || rawHeaders.isBlank()) {
            return Map.of("Content-Type", "application/json");
        }
        try {
            Map<String, Object> headers = objectMapper.readValue(rawHeaders, new TypeReference<>() { });
            Map<String, String> result = new LinkedHashMap<>();
            headers.forEach((key, value) -> result.put(key, String.valueOf(value)));
            return result;
        } catch (Exception exception) {
            return Map.of("Content-Type", "application/json");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizedType(String value) {
        if (value == null) {
            return "string";
        }
        return switch (value.toLowerCase()) {
            case "int", "integer", "long" -> "integer";
            case "float", "double", "decimal", "number" -> "number";
            case "bool", "boolean" -> "boolean";
            case "array", "list" -> "array";
            case "object", "map" -> "object";
            default -> "string";
        };
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String result = String.valueOf(value);
        return result.isBlank() ? null : result;
    }

    private int intValue(Object value, int fallback) {
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
}
