package com.feng.storage.service.api;

public class ChunkUploadRequest {
    private String uploadId;
    private Integer chunkNumber;

    public ChunkUploadRequest() {}

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public Integer getChunkNumber() { return chunkNumber; }
    public void setChunkNumber(Integer chunkNumber) { this.chunkNumber = chunkNumber; }
}


