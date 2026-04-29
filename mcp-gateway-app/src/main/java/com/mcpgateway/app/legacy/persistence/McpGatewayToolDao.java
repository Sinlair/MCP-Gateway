package com.mcpgateway.app.legacy.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface McpGatewayToolDao {

    void insert(McpGatewayToolPO po);

    int updateProtocolByGatewayId(McpGatewayToolPO po);

    List<McpGatewayToolPO> queryListByGatewayId(String gatewayId);

    List<McpGatewayToolPO> queryToolList(McpGatewayToolPO query);

    Long queryToolListCount(McpGatewayToolPO query);

    List<McpGatewayToolPO> queryAll();

    void deleteByToolId(Long toolId);
}
