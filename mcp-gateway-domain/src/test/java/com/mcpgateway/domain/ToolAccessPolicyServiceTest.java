package com.mcpgateway.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcpgateway.domain.accesscontrol.model.PolicyDecision;
import com.mcpgateway.domain.accesscontrol.model.PolicySubjectType;
import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicy;
import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicyCommand;
import com.mcpgateway.domain.accesscontrol.repository.ToolAccessPolicyRepository;
import com.mcpgateway.domain.accesscontrol.service.ToolAccessPolicyService;
import com.mcpgateway.domain.security.model.ClientRole;
import com.mcpgateway.domain.security.model.GatewayClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ToolAccessPolicyServiceTest {

    @Test
    void shouldDenyWhenMatchingDenyPolicyExists() {
        ToolAccessPolicyService service = new ToolAccessPolicyService(new InMemoryRepository());
        GatewayClient client = new GatewayClient(
                "demo-app",
                "Demo App",
                "app-key",
                "app-token",
                Set.of(ClientRole.APP),
                true
        );
        service.register(new ToolAccessPolicyCommand(
                "dev",
                PolicySubjectType.CLIENT,
                "demo-app",
                "weather:forecast",
                PolicyDecision.DENY,
                true,
                "blocked"
        ));

        assertFalse(service.isAllowed("dev", client, "weather:forecast"));
    }

    @Test
    void shouldRequireExplicitAllowWhenAllowModeExists() {
        ToolAccessPolicyService service = new ToolAccessPolicyService(new InMemoryRepository());
        GatewayClient client = new GatewayClient(
                "demo-app",
                "Demo App",
                "app-key",
                "app-token",
                Set.of(ClientRole.APP),
                true
        );
        service.register(new ToolAccessPolicyCommand(
                "dev",
                PolicySubjectType.ROLE,
                "APP",
                "weather:forecast",
                PolicyDecision.ALLOW,
                true,
                "allow forecast only"
        ));

        assertTrue(service.isAllowed("dev", client, "weather:forecast"));
        assertFalse(service.isAllowed("dev", client, "weather:alerts"));
    }

    private static final class InMemoryRepository implements ToolAccessPolicyRepository {

        private final List<ToolAccessPolicy> items = new ArrayList<>();

        @Override
        public ToolAccessPolicy save(ToolAccessPolicy toolAccessPolicy) {
            items.add(toolAccessPolicy);
            return toolAccessPolicy;
        }

        @Override
        public List<ToolAccessPolicy> findAllByEnvironment(String environment) {
            return items.stream().filter(item -> item.environment().equals(environment)).toList();
        }
    }
}

