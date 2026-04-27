package com.mcpgateway.domain.accesscontrol.repository;

import com.mcpgateway.domain.accesscontrol.model.ToolAccessPolicy;
import java.util.List;

public interface ToolAccessPolicyRepository {

    ToolAccessPolicy save(ToolAccessPolicy toolAccessPolicy);

    List<ToolAccessPolicy> findAllByEnvironment(String environment);
}

