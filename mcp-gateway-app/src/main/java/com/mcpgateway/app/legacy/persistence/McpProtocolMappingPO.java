package com.mcpgateway.app.legacy.persistence;

import java.time.LocalDateTime;

public class McpProtocolMappingPO {

    private Long id;
    private Long protocolId;
    private String mappingType;
    private String parentPath;
    private String fieldName;
    private String mcpPath;
    private String mcpType;
    private String mcpDesc;
    private Integer isRequired;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(Long protocolId) {
        this.protocolId = protocolId;
    }

    public String getMappingType() {
        return mappingType;
    }

    public void setMappingType(String mappingType) {
        this.mappingType = mappingType;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getMcpPath() {
        return mcpPath;
    }

    public void setMcpPath(String mcpPath) {
        this.mcpPath = mcpPath;
    }

    public String getMcpType() {
        return mcpType;
    }

    public void setMcpType(String mcpType) {
        this.mcpType = mcpType;
    }

    public String getMcpDesc() {
        return mcpDesc;
    }

    public void setMcpDesc(String mcpDesc) {
        this.mcpDesc = mcpDesc;
    }

    public Integer getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Integer isRequired) {
        this.isRequired = isRequired;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
