package com.mcpgateway.app.legacy.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface McpGatewayDao {

    void insert(McpGatewayPO po);

    int updateAuthStatusByGatewayId(McpGatewayPO po);

    List<McpGatewayPO> queryAll();

    McpGatewayPO queryMcpGatewayByGatewayId(String gatewayId);

    List<McpGatewayPO> queryGatewayList(McpGatewayPO query);

    Long queryGatewayListCount(McpGatewayPO query);
}
