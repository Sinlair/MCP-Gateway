package com.mcpgateway.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.domain.security.service.ClientAuthenticationService;
import com.mcpgateway.domain.tool.model.ToolRegistrationCommand;
import com.mcpgateway.domain.tool.service.ToolCatalogService;
import com.mcpgateway.domain.upstream.model.TransportType;
import com.mcpgateway.domain.upstream.model.UpstreamRegistrationCommand;
import com.mcpgateway.domain.upstream.service.UpstreamRegistrationService;
import com.mcpgateway.trigger.http.mcp.McpProtocolService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class McpProtocolMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UpstreamRegistrationService upstreamRegistrationService;

    @Autowired
    private ToolCatalogService toolCatalogService;

    @Autowired
    private ClientAuthenticationService clientAuthenticationService;

    @Autowired
    private McpProtocolService mcpProtocolService;

    @Test
    void shouldOpenMcpSseStreamWithSessionCookie() throws Exception {
        mockMvc.perform(get("/mcp")
                        .param("api_key", "demo-app-key")
                        .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(header().string("X-Client-Id", "demo-app"))
                .andExpect(header().string("Set-Cookie", containsString("MCP_SESSION_ID=")));
    }

    @Test
    void shouldHandleStandardMcpInitializeToolsListAndToolsCall() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String serverCode = "weather-" + suffix;
        String toolIdentifier = serverCode + ":forecast";

        upstreamRegistrationService.register(new UpstreamRegistrationCommand(
                "dev",
                serverCode,
                "Weather Server " + suffix,
                "https://weather.example.com/mcp",
                TransportType.HTTP,
                "API_KEY",
                true,
                3000
        ));
        upstreamRegistrationService.refreshStatus("dev", serverCode);
        toolCatalogService.register(new ToolRegistrationCommand(
                "dev",
                serverCode,
                "forecast",
                "Return mock forecast",
                "{\"city\":\"string\",\"days\":\"integer\"}",
                true
        ));

        GatewayClient client = clientAuthenticationService.authenticate("demo-admin-key", null);

        JsonNode initializeResponse = mcpProtocolService.handleRequest(
                client,
                "dev",
                "session-1",
                "request-1",
                rpcRequest("initialize", objectMapper.createObjectNode()
                        .putObject("clientInfo")
                        .put("name", "Protocol Test Client"))
        );

        assertThat(initializeResponse.path("result").path("protocolVersion").asText())
                .isEqualTo("2024-11-05");
        assertThat(initializeResponse.path("result").path("capabilities").path("tools").path("listChanged").asBoolean())
                .isTrue();
        assertThat(initializeResponse.path("result").path("tools").toString())
                .contains(toolIdentifier);

        JsonNode listResponse = mcpProtocolService.handleRequest(
                client,
                "dev",
                "session-1",
                "request-2",
                rpcRequest("tools/list", objectMapper.createObjectNode())
        );

        assertThat(listResponse.path("result").path("tools").isArray()).isTrue();
        assertThat(listResponse.path("result").path("tools").get(0).path("name").asText())
                .isEqualTo(toolIdentifier);
        assertThat(listResponse.path("result").path("tools").get(0).path("inputSchema").path("properties").path("city").path("type").asText())
                .isEqualTo("string");

        ObjectNode callParams = objectMapper.createObjectNode();
        callParams.put("name", toolIdentifier);
        callParams.set("arguments", objectMapper.createObjectNode()
                .put("city", "Shanghai")
                .put("days", 2));

        JsonNode callResponse = mcpProtocolService.handleRequest(
                client,
                "dev",
                "session-1",
                "request-3",
                rpcRequest("tools/call", callParams)
        );

        assertThat(callResponse.path("result").path("isError").asBoolean()).isFalse();
        assertThat(callResponse.path("result").path("structuredContent").path("mode").asText())
                .isEqualTo("mock");
        assertThat(callResponse.path("result").path("structuredContent").path("tool").asText())
                .isEqualTo("forecast");
    }

    private JsonNode rpcRequest(String method, JsonNode params) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", method);
        request.set("params", params);
        return request;
    }
}
