package com.mcpgateway.app.legacy.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface McpProtocolMappingDao {

    void insert(McpProtocolMappingPO po);

    void deleteByProtocolId(Long protocolId);

    List<McpProtocolMappingPO> queryMcpGatewayToolConfigListByProtocolId(Long protocolId);

    List<McpProtocolMappingPO> queryListByProtocolIds(List<Long> protocolIds);
}
