package com.mcpgateway.domain.tool.repository;

import com.mcpgateway.domain.tool.model.ToolDefinition;
import java.util.List;
import java.util.Optional;

public interface ToolDefinitionRepository {

    ToolDefinition save(ToolDefinition toolDefinition);

    List<ToolDefinition> findAllByEnvironment(String environment);

    List<ToolDefinition> findAllByEnvironmentAndServerCode(String environment, String serverCode);

    Optional<ToolDefinition> findByEnvironmentAndIdentifier(String environment, String toolIdentifier);
}

