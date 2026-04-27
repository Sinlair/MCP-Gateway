package com.mcpgateway.trigger.http.admin.policy.dto;

import com.mcpgateway.domain.accesscontrol.model.PolicyDecision;
import com.mcpgateway.domain.accesscontrol.model.PolicySubjectType;
import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicyCommand;

public record ToolAccessPolicyRequest(
        String environment,
        PolicySubjectType subjectType,
        String subjectId,
        String toolIdentifier,
        PolicyDecision decision,
        boolean enabled,
        String reason
) {

    public ToolAccessPolicyCommand toCommand() {
        return new ToolAccessPolicyCommand(
                environment,
                subjectType,
                subjectId,
                toolIdentifier,
                decision,
                enabled,
                reason
        );
    }
}

