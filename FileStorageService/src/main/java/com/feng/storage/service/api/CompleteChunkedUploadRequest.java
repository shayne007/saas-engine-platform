package com.feng.storage.service.api;

import java.util.List;

public class CompleteChunkedUploadRequest {
    private String uploadId;
    private List<ChunkInfo> chunks;

    public CompleteChunkedUploadRequest() {}

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public List<ChunkInfo> getChunks() { return chunks; }
    public void setChunks(List<ChunkInfo> chunks) { this.chunks = chunks; }
}


