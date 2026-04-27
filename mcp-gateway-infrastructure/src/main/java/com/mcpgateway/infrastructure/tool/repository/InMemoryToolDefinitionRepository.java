package com.mcpgateway.infrastructure.tool.repository;

import com.mcpgateway.domain.tool.model.ToolDefinition;
import com.mcpgateway.domain.tool.repository.ToolDefinitionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryToolDefinitionRepository implements ToolDefinitionRepository {

    private final List<ToolDefinition> items = new ArrayList<>();

    @Override
    public synchronized ToolDefinition save(ToolDefinition toolDefinition) {
        items.removeIf(existing ->
                existing.environment().equals(toolDefinition.environment())
                        && existing.toolIdentifier().equals(toolDefinition.toolIdentifier())
        );
        items.add(toolDefinition);
        return toolDefinition;
    }

    @Override
    public synchronized List<ToolDefinition> findAllByEnvironment(String environment) {
        return items.stream()
                .filter(item -> item.environment().equals(environment))
                .sorted(Comparator.comparing(ToolDefinition::toolIdentifier))
                .toList();
    }

    @Override
    public synchronized List<ToolDefinition> findAllByEnvironmentAndServerCode(String environment, String serverCode) {
        return items.stream()
                .filter(item -> item.environment().equals(environment) && item.serverCode().equals(serverCode))
                .sorted(Comparator.comparing(ToolDefinition::toolName))
                .toList();
    }

    @Override
    public synchronized Optional<ToolDefinition> findByEnvironmentAndIdentifier(String environment, String toolIdentifier) {
        return items.stream()
                .filter(item -> item.environment().equals(environment) && item.toolIdentifier().equals(toolIdentifier))
                .findFirst();
    }
}

