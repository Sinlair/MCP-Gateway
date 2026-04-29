package com.mcpgateway.app.legacy.persistence;

public class LegacyPagePO {

    private Integer page;
    private Integer rows;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getLimitStart() {
        if (page == null || rows == null) {
            return null;
        }
        return Math.max(page - 1, 0) * rows;
    }

    public Integer getLimitCount() {
        return rows;
    }
}
