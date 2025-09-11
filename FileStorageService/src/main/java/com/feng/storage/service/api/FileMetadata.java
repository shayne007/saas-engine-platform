package com.feng.storage.service.api;

import java.time.Instant;
import java.util.Map;

public class FileMetadata {
    private String id;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private String fileHash;
    private String gcsBucket;
    private String gcsObjectKey;
    private String uploadStatus;
    private String createdBy;
    private String projectId;
    private Map<String, String> tags;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public FileMetadata() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getGcsBucket() { return gcsBucket; }
    public void setGcsBucket(String gcsBucket) { this.gcsBucket = gcsBucket; }
    public String getGcsObjectKey() { return gcsObjectKey; }
    public void setGcsObjectKey(String gcsObjectKey) { this.gcsObjectKey = gcsObjectKey; }
    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}


