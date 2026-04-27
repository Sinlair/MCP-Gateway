package com.mcpgateway.app.config;

import com.mcpgateway.domain.accesscontrol.repository.ToolAccessPolicyRepository;
import com.mcpgateway.domain.accesscontrol.service.ToolAccessPolicyService;
import com.mcpgateway.domain.security.repository.GatewayClientRepository;
import com.mcpgateway.domain.security.service.ClientAuthenticationService;
import com.mcpgateway.domain.tool.repository.ToolDefinitionRepository;
import com.mcpgateway.domain.tool.repository.ToolExecutor;
import com.mcpgateway.domain.tool.service.GatewayToolService;
import com.mcpgateway.domain.tool.service.ToolCatalogService;
import com.mcpgateway.domain.upstream.repository.UpstreamHealthProbe;
import com.mcpgateway.domain.upstream.repository.UpstreamServerRepository;
import com.mcpgateway.domain.upstream.service.UpstreamRegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanFactoryConfiguration {

    @Bean
    public UpstreamRegistrationService upstreamRegistrationService(
            UpstreamServerRepository upstreamServerRepository,
            UpstreamHealthProbe upstreamHealthProbe
    ) {
        return new UpstreamRegistrationService(upstreamServerRepository, upstreamHealthProbe);
    }

    @Bean
    public ClientAuthenticationService clientAuthenticationService(
            GatewayClientRepository gatewayClientRepository
    ) {
        return new ClientAuthenticationService(gatewayClientRepository);
    }

    @Bean
    public ToolAccessPolicyService toolAccessPolicyService(
            ToolAccessPolicyRepository toolAccessPolicyRepository
    ) {
        return new ToolAccessPolicyService(toolAccessPolicyRepository);
    }

    @Bean
    public ToolCatalogService toolCatalogService(
            ToolDefinitionRepository toolDefinitionRepository,
            UpstreamServerRepository upstreamServerRepository
    ) {
        return new ToolCatalogService(toolDefinitionRepository, upstreamServerRepository);
    }

    @Bean
    public GatewayToolService gatewayToolService(
            ToolCatalogService toolCatalogService,
            ToolDefinitionRepository toolDefinitionRepository,
            UpstreamServerRepository upstreamServerRepository,
            ToolAccessPolicyService toolAccessPolicyService,
            ToolExecutor toolExecutor
    ) {
        return new GatewayToolService(
                toolCatalogService,
                toolDefinitionRepository,
                upstreamServerRepository,
                toolAccessPolicyService,
                toolExecutor
        );
    }
}
