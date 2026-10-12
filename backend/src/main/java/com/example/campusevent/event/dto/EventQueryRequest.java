package com.example.campusevent.event.dto;

public class EventQueryRequest {
    private long page = 1;
    private long pageSize = 10;
    private String keyword;
    private String status;
    private Boolean mine;

    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getPageSize() { return pageSize; }
    public void setPageSize(long pageSize) { this.pageSize = pageSize; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            this.keyword = null;
        } else {
            this.keyword = keyword.trim();
        }
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getMine() { return mine; }
    public void setMine(Boolean mine) { this.mine = mine; }
}
