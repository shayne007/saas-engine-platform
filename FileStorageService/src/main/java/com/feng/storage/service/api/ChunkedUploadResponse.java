package com.feng.storage.service.api;

import java.time.Instant;

public class ChunkedUploadResponse {
    private String fileId;
    private String uploadId;
    private Instant expiresAt;

    public ChunkedUploadResponse() {}

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}


