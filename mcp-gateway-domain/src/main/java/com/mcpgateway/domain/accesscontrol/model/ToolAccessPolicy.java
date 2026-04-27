package com.mcpgateway.domain.accesscontrol.model;

import com.mcpgateway.domain.security.model.GatewayClient;
import com.mcpgateway.types.enums.ResponseCode;
import com.mcpgateway.types.exception.AppException;

public record ToolAccessPolicy(
        String environment,
        PolicySubjectType subjectType,
        String subjectId,
        String toolIdentifier,
        PolicyDecision decision,
        boolean enabled,
        String reason
) {

    public ToolAccessPolicy {
        if (isBlank(environment) || subjectType == null || isBlank(subjectId)
                || isBlank(toolIdentifier) || decision == null) {
            throw new AppException(ResponseCode.BAD_REQUEST, "invalid access policy definition");
        }
    }

    public static ToolAccessPolicy register(ToolAccessPolicyCommand command) {
        return new ToolAccessPolicy(
                command.environment(),
                command.subjectType(),
                command.subjectId(),
                command.toolIdentifier(),
                command.decision(),
                command.enabled(),
                command.reason()
        );
    }

    public boolean appliesTo(GatewayClient client) {
        return switch (subjectType) {
            case CLIENT -> client.clientId().equals(subjectId);
            case ROLE -> client.roles().stream().anyMatch(role -> role.name().equalsIgnoreCase(subjectId));
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

