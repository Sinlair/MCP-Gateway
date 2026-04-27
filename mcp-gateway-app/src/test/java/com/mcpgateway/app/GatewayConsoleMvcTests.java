package com.mcpgateway.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayConsoleMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectMissingCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/gateway/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0401"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void shouldSupportAdminLifecycleAndPolicyAwareInvocation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/upstreams")
                        .header("X-API-Key", "demo-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment":"dev",
                                  "serverCode":"weather",
                                  "name":"Weather Server",
                                  "baseUrl":"https://weather.example.com/mcp",
                                  "transportType":"HTTP",
                                  "authMode":"API_KEY",
                                  "enabled":true,
                                  "timeoutMs":3000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serverCode").value("weather"));

        mockMvc.perform(post("/api/v1/admin/upstreams/weather/refresh?environment=dev")
                        .header("X-API-Key", "demo-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthStatus").value("UP"));

        mockMvc.perform(post("/api/v1/admin/tools")
                        .header("X-API-Key", "demo-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment":"dev",
                                  "serverCode":"weather",
                                  "toolName":"forecast",
                                  "description":"Return mock forecast",
                                  "inputSchema":"{\\"city\\":\\"string\\"}",
                                  "enabled":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolIdentifier").value("weather:forecast"));

        mockMvc.perform(get("/api/v1/gateway/tools?environment=dev")
                        .header("X-API-Key", "demo-app-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].toolIdentifier").value("weather:forecast"));

        mockMvc.perform(post("/api/v1/admin/policies")
                        .header("X-API-Key", "demo-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment":"dev",
                                  "subjectType":"CLIENT",
                                  "subjectId":"demo-app",
                                  "toolIdentifier":"weather:forecast",
                                  "decision":"DENY",
                                  "enabled":true,
                                  "reason":"blocked for test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("DENY"));

        mockMvc.perform(get("/api/v1/gateway/tools?environment=dev")
                        .header("X-API-Key", "demo-app-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(post("/api/v1/gateway/tools/invoke")
                        .header("X-API-Key", "demo-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment":"dev",
                                  "toolIdentifier":"weather:forecast",
                                  "arguments":{"city":"Shanghai","days":2}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.output.mode").value("mock"));

        mockMvc.perform(post("/api/v1/gateway/tools/invoke")
                        .header("X-API-Key", "demo-app-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment":"dev",
                                  "toolIdentifier":"weather:forecast",
                                  "arguments":{"city":"Shanghai","days":2}
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0403"));
    }

    @Test
    void shouldExposeBigMarketManagedSystemOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/systems/big-market")
                        .header("X-API-Key", "demo-admin-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemName").value("big-market-71772-z"))
                .andExpect(jsonPath("$.data.repoPath").value("E:\\Internship\\program\\big-market-71772-z"))
                .andExpect(jsonPath("$.data.baseUrl").value("http://127.0.0.1:8091"))
                .andExpect(jsonPath("$.data.supportedOperations.length()").value(5));
    }

    @Test
    void shouldIssueConsoleTokenAndUseItForSessionAndAdminOverview() throws Exception {
        MvcResult issueResult = mockMvc.perform(post("/api/v1/public/console/tokens/demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile":"demo-admin",
                                  "environment":"dev",
                                  "managedSystems":["big-market-71772-z"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.tokenId").isNotEmpty())
                .andExpect(jsonPath("$.data.scopes.length()").value(8))
                .andReturn();

        JsonNode issuePayload = objectMapper.readTree(issueResult.getResponse().getContentAsString());
        String accessToken = issuePayload.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/console/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile").value("demo-admin"))
                .andExpect(jsonPath("$.data.environment").value("dev"))
                .andExpect(jsonPath("$.data.managedSystems[0]").value("big-market-71772-z"));

        mockMvc.perform(get("/api/v1/admin/systems/big-market")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemName").value("big-market-71772-z"));

        mockMvc.perform(get("/api/v1/admin/console/audits")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").isNotEmpty());

        mockMvc.perform(post("/api/v1/console/tokens/revoke")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/v1/console/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0401"));
    }
}
