package com.mcpgateway.trigger.http.admin.policy.dto;

import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicy;

public record ToolAccessPolicyResponse(
        String environment,
        String subjectType,
        String subjectId,
        String toolIdentifier,
        String decision,
        boolean enabled,
        String reason
) {

    public static ToolAccessPolicyResponse from(ToolAccessPolicy toolAccessPolicy) {
        return new ToolAccessPolicyResponse(
                toolAccessPolicy.environment(),
                toolAccessPolicy.subjectType().name(),
                toolAccessPolicy.subjectId(),
                toolAccessPolicy.toolIdentifier(),
                toolAccessPolicy.decision().name(),
                toolAccessPolicy.enabled(),
                toolAccessPolicy.reason()
        );
    }
}

