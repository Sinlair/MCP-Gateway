package com.mcpgateway.trigger.http.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpgateway.domain.gateway.model.ToolInvocationCommand;
import com.mcpgateway.domain.gateway.model.ToolInvocationResult;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.tool.service.GatewayToolService;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class McpProtocolService {

    private static final String JSON_RPC_VERSION = "2.0";
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";

    private final GatewayToolService gatewayToolService;
    private final ObjectMapper objectMapper;
    private final String applicationName;

    public McpProtocolService(
            GatewayToolService gatewayToolService,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:mcp-gateway}") String applicationName
    ) {
        this.gatewayToolService = gatewayToolService;
        this.objectMapper = objectMapper;
        this.applicationName = applicationName;
    }

    public ObjectNode handleRequest(
            GatewayClient client,
            String environment,
            String sessionId,
            String requestId,
            JsonNode payload
    ) {
        if (payload == null || !payload.isObject()) {
            return errorResponse(NullNode.instance, -32600, "invalid request payload");
        }

        JsonNode idNode = payload.get("id");
        JsonNode methodNode = payload.get("method");
        if (methodNode == null || !methodNode.isTextual()) {
            return errorResponse(idOrNull(idNode), -32600, "method is required");
        }

        String method = methodNode.asText();
        JsonNode params = payload.path("params");

        try {
            return switch (method) {
                case "initialize" -> successResponse(
                        idOrNull(idNode),
                        buildInitializeResult(client, environment)
                );
                case "tools/list" -> successResponse(
                        idOrNull(idNode),
                        Map.of("tools", buildToolDescriptors(environment, client))
                );
                case "tools/call" -> successResponse(
                        idOrNull(idNode),
                        invokeTool(client, environment, sessionId, requestId, params)
                );
                case "resources/list" -> successResponse(
                        idOrNull(idNode),
                        Map.of("resources", List.of())
                );
                case "resources/read" -> successResponse(
                        idOrNull(idNode),
                        Map.of("contents", List.of())
                );
                case "prompts/list" -> successResponse(
                        idOrNull(idNode),
                        Map.of("prompts", List.of())
                );
                case "ping", "notifications/initialized" -> null;
                default -> errorResponse(idOrNull(idNode), -32601, "method not found: " + method);
            };
        } catch (AppException exception) {
            return errorResponse(
                    idOrNull(idNode),
                    toJsonRpcErrorCode(exception.getCode()),
                    exception.getMessage()
            );
        } catch (IllegalArgumentException exception) {
            return errorResponse(idOrNull(idNode), -32602, exception.getMessage());
        } catch (Exception exception) {
            return errorResponse(idOrNull(idNode), -32603, exception.getMessage());
        }
    }

    private Map<String, Object> buildInitializeResult(GatewayClient client, String environment) {
        List<Map<String, Object>> tools = buildToolDescriptors(environment, client);
        return Map.of(
                "protocolVersion", MCP_PROTOCOL_VERSION,
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", true),
                        "resources", Map.of("listChanged", false, "subscribe", false),
                        "prompts", Map.of("listChanged", false)
                ),
                "serverInfo", Map.of(
                        "name", applicationName,
                        "version", resolveVersion()
                ),
                "instructions", "MCP Gateway exposes policy-aware tool discovery and invocation.",
                "tools", tools,
                "resources", List.of(),
                "prompts", List.of()
        );
    }

    private List<Map<String, Object>> buildToolDescriptors(String environment, GatewayClient client) {
        List<ToolDefinition> tools = gatewayToolService.discover(environment, client);
        List<Map<String, Object>> descriptors = new ArrayList<>(tools.size());
        for (ToolDefinition tool : tools) {
            descriptors.add(Map.of(
                    "name", tool.toolIdentifier(),
                    "description", tool.description() == null ? "" : tool.description(),
                    "inputSchema", normalizeInputSchema(tool.inputSchema())
            ));
        }
        return descriptors;
    }

    private Map<String, Object> invokeTool(
            GatewayClient client,
            String environment,
            String sessionId,
            String requestId,
            JsonNode params
    ) {
        if (!params.isObject()) {
            throw new IllegalArgumentException("tools/call params must be an object");
        }
        String toolName = params.path("name").asText(null);
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("tools/call requires a tool name");
        }
        Map<String, Object> arguments = params.has("arguments")
                ? objectMapper.convertValue(params.get("arguments"), new TypeReference<>() { })
                : Map.of();

        ToolInvocationResult result = gatewayToolService.invoke(
                client,
                requestId,
                sessionId,
                new ToolInvocationCommand(environment, toolName, arguments)
        );

        String text = summarizeOutput(result);
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", text)),
                "structuredContent", result.output(),
                "isError", !"SUCCESS".equalsIgnoreCase(result.status())
        );
    }

    private JsonNode normalizeInputSchema(String inputSchema) {
        if (inputSchema == null || inputSchema.isBlank()) {
            return emptyObjectSchema();
        }
        try {
            JsonNode node = objectMapper.readTree(inputSchema);
            if (looksLikeJsonSchema(node)) {
                return ensureObjectSchema(node.deepCopy());
            }
            if (node.isObject()) {
                return simplifiedObjectSchema((ObjectNode) node);
            }
            return emptyObjectSchema();
        } catch (JsonProcessingException exception) {
            return emptyObjectSchema();
        }
    }

    private JsonNode ensureObjectSchema(JsonNode node) {
        if (!node.isObject()) {
            return emptyObjectSchema();
        }
        ObjectNode objectNode = (ObjectNode) node;
        if (!objectNode.has("type")) {
            objectNode.put("type", "object");
        }
        if (!objectNode.has("additionalProperties")) {
            objectNode.put("additionalProperties", false);
        }
        return objectNode;
    }

    private boolean looksLikeJsonSchema(JsonNode node) {
        return node.isObject() && (
                node.has("type")
                        || node.has("properties")
                        || node.has("required")
                        || node.has("$schema")
        );
    }

    private ObjectNode simplifiedObjectSchema(ObjectNode source) {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");

        Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            properties.set(entry.getKey(), simplifiedFieldSchema(entry.getValue()));
        }
        return schema;
    }

    private JsonNode simplifiedFieldSchema(JsonNode value) {
        if (value == null || value.isNull()) {
            return primitiveSchema("string");
        }
        if (value.isTextual()) {
            return primitiveSchema(value.asText());
        }
        if (value.isObject()) {
            if (looksLikeJsonSchema(value)) {
                return ensureObjectSchema(value.deepCopy());
            }
            return simplifiedObjectSchema((ObjectNode) value);
        }
        if (value.isArray()) {
            ObjectNode schema = JsonNodeFactory.instance.objectNode();
            schema.put("type", "array");
            ArrayNode arrayNode = (ArrayNode) value;
            schema.set(
                    "items",
                    arrayNode.isEmpty() ? primitiveSchema("string") : simplifiedFieldSchema(arrayNode.get(0))
            );
            return schema;
        }
        if (value.isBoolean()) {
            return primitiveSchema("boolean");
        }
        if (value.isInt() || value.isLong()) {
            return primitiveSchema("integer");
        }
        if (value.isFloat() || value.isDouble() || value.isBigDecimal()) {
            return primitiveSchema("number");
        }
        return primitiveSchema("string");
    }

    private ObjectNode primitiveSchema(String type) {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", normalizePrimitiveType(type));
        return schema;
    }

    private String normalizePrimitiveType(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "number", "float", "double", "decimal" -> "number";
            case "int", "integer", "long" -> "integer";
            case "boolean", "bool" -> "boolean";
            case "array", "list" -> "array";
            case "object", "map" -> "object";
            default -> "string";
        };
    }

    private ObjectNode emptyObjectSchema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.putObject("properties");
        return schema;
    }

    private String summarizeOutput(ToolInvocationResult result) {
        Object summary = result.output().get("summary");
        if (summary != null) {
            return String.valueOf(summary);
        }
        try {
            return objectMapper.writeValueAsString(result.output());
        } catch (JsonProcessingException exception) {
            return result.toolIdentifier() + " completed with status " + result.status();
        }
    }

    private ObjectNode successResponse(JsonNode id, Object result) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.set("id", idOrNull(id));
        response.set("result", objectMapper.valueToTree(result));
        return response;
    }

    private ObjectNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.set("id", idOrNull(id));
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return response;
    }

    private JsonNode idOrNull(JsonNode idNode) {
        return idNode == null ? NullNode.instance : idNode.deepCopy();
    }

    private int toJsonRpcErrorCode(String responseCode) {
        if (ResponseCode.BAD_REQUEST.code().equals(responseCode)) {
            return -32602;
        }
        if (ResponseCode.NOT_FOUND.code().equals(responseCode)) {
            return -32601;
        }
        if (ResponseCode.UNAUTHORIZED.code().equals(responseCode)
                || ResponseCode.FORBIDDEN.code().equals(responseCode)) {
            return -32001;
        }
        if (ResponseCode.CONFLICT.code().equals(responseCode)) {
            return -32002;
        }
        return -32603;
    }

    private String resolveVersion() {
        Package currentPackage = getClass().getPackage();
        if (currentPackage != null && currentPackage.getImplementationVersion() != null) {
            return currentPackage.getImplementationVersion();
        }
        return "0.0.1";
    }
}
