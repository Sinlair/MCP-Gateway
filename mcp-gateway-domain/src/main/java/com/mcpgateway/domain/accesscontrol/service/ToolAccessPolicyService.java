package com.mcpgateway.domain.accesscontrol.service;

import com.mcpgateway.domain.accesscontrol.model.PolicyDecision;
import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicy;
import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicyCommand;
import com.mcpgateway.domain.accesscontrol.repository.ToolAccessPolicyRepository;
import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;
import java.util.List;

public class ToolAccessPolicyService {

    private final ToolAccessPolicyRepository toolAccessPolicyRepository;

    public ToolAccessPolicyService(ToolAccessPolicyRepository toolAccessPolicyRepository) {
        this.toolAccessPolicyRepository = toolAccessPolicyRepository;
    }

    public ToolAccessPolicy register(ToolAccessPolicyCommand command) {
        return toolAccessPolicyRepository.save(ToolAccessPolicy.register(command));
    }

    public List<ToolAccessPolicy> listByEnvironment(String environment) {
        return toolAccessPolicyRepository.findAllByEnvironment(environment);
    }

    public boolean isAllowed(String environment, GatewayClient client, String toolIdentifier) {
        List<ToolAccessPolicy> matchedPolicies = listByEnvironment(environment).stream()
                .filter(ToolAccessPolicy::enabled)
                .filter(policy -> policy.toolIdentifier().equals(toolIdentifier))
                .filter(policy -> policy.appliesTo(client))
                .toList();

        boolean denied = matchedPolicies.stream().anyMatch(policy -> policy.decision() == PolicyDecision.DENY);
        if (denied) {
            return false;
        }

        boolean hasAllowMode = listByEnvironment(environment).stream()
                .filter(ToolAccessPolicy::enabled)
                .filter(policy -> policy.appliesTo(client))
                .anyMatch(policy -> policy.decision() == PolicyDecision.ALLOW);
        if (!hasAllowMode) {
            return true;
        }
        return matchedPolicies.stream().anyMatch(policy -> policy.decision() == PolicyDecision.ALLOW);
    }

    public void requireAllowed(String environment, GatewayClient client, String toolIdentifier) {
        if (!isAllowed(environment, client, toolIdentifier)) {
            throw new AppException(
                    ResponseCode.FORBIDDEN,
                    "client is not allowed to invoke tool: " + toolIdentifier
            );
        }
    }
}

