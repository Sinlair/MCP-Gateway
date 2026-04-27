package com.mcpgateway.infrastructure.accesscontrol.repository;

import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicy;
import com.mcpgateway.domain.accesscontrol.repository.ToolAccessPolicyRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryToolAccessPolicyRepository implements ToolAccessPolicyRepository {

    private final List<ToolAccessPolicy> items = new ArrayList<>();

    @Override
    public synchronized ToolAccessPolicy save(ToolAccessPolicy toolAccessPolicy) {
        items.removeIf(existing ->
                existing.environment().equals(toolAccessPolicy.environment())
                        && existing.subjectType() == toolAccessPolicy.subjectType()
                        && existing.subjectId().equals(toolAccessPolicy.subjectId())
                        && existing.toolIdentifier().equals(toolAccessPolicy.toolIdentifier())
        );
        items.add(toolAccessPolicy);
        return toolAccessPolicy;
    }

    @Override
    public synchronized List<ToolAccessPolicy> findAllByEnvironment(String environment) {
        return items.stream()
                .filter(item -> item.environment().equals(environment))
                .toList();
    }
}

