package com.feng.storage.service.api;

import java.time.Instant;
import java.util.List;

public class FileQueryRequest {
    private String projectId;
    private List<String> mimeTypes;
    private String createdBy;
    private Instant createdAfter;
    private Instant createdBefore;
    private Integer page;
    private Integer size;
    private String sort;

    public FileQueryRequest() {}

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public List<String> getMimeTypes() { return mimeTypes; }
    public void setMimeTypes(List<String> mimeTypes) { this.mimeTypes = mimeTypes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAfter() { return createdAfter; }
    public void setCreatedAfter(Instant createdAfter) { this.createdAfter = createdAfter; }
    public Instant getCreatedBefore() { return createdBefore; }
    public void setCreatedBefore(Instant createdBefore) { this.createdBefore = createdBefore; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}


