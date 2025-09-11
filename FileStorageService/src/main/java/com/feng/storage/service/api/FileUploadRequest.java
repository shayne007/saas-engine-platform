package com.feng.storage.service.api;

import java.util.Map;

public class FileUploadRequest {
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private String fileHash;
    private String userId;
    private String projectId;
    private Map<String, String> tags;
    private Map<String, Object> metadata;
    private Boolean allowDeduplication = Boolean.TRUE;

    public FileUploadRequest() {}

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Boolean getAllowDeduplication() { return allowDeduplication; }
    public void setAllowDeduplication(Boolean allowDeduplication) { this.allowDeduplication = allowDeduplication; }
}


