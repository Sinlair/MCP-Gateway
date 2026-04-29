package com.mcpgateway.app.legacy.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface McpProtocolHttpDao {

    void insert(McpProtocolHttpPO po);

    void updateByProtocolId(McpProtocolHttpPO po);

    void deleteByProtocolId(Long protocolId);

    List<McpProtocolHttpPO> queryAll();

    McpProtocolHttpPO queryMcpProtocolHttpByProtocolId(Long protocolId);

    List<McpProtocolHttpPO> queryListByProtocolIds(List<Long> protocolIds);

    List<McpProtocolHttpPO> queryProtocolList(McpProtocolHttpPO query);

    Long queryProtocolListCount(McpProtocolHttpPO query);
}
