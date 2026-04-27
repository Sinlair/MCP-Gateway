package com.mcpgateway.domain.accesscontrol.model;

public record ToolAccessPolicyCommand(
        String environment,
        PolicySubjectType subjectType,
        String subjectId,
        String toolIdentifier,
        PolicyDecision decision,
        boolean enabled,
        String reason
) {
}

