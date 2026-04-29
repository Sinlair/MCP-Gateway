package com.mcpgateway.app.legacy.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface McpGatewayAuthDao {

    void insert(McpGatewayAuthPO po);

    void updateByGatewayId(McpGatewayAuthPO po);

    void deleteByGatewayId(String gatewayId);

    List<McpGatewayAuthPO> queryAll();

    McpGatewayAuthPO queryMcpGatewayAuthPO(McpGatewayAuthPO query);

    Integer queryEffectiveGatewayAuthCount(String gatewayId);

    List<McpGatewayAuthPO> queryAuthList(McpGatewayAuthPO query);

    Long queryAuthListCount(McpGatewayAuthPO query);

    List<McpGatewayAuthPO> queryListByGatewayId(String gatewayId);
}
