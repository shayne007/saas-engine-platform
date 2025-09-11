package com.feng.storage.service.api;

import java.time.Instant;

public class ChunkUploadResponse {
    private String chunkUploadUrl;
    private Instant expiresAt;

    public ChunkUploadResponse() {}

    public String getChunkUploadUrl() { return chunkUploadUrl; }
    public void setChunkUploadUrl(String chunkUploadUrl) { this.chunkUploadUrl = chunkUploadUrl; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}


