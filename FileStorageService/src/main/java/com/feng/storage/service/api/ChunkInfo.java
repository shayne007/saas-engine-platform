package com.feng.storage.service.api;

public class ChunkInfo {
    private Integer chunkNumber;
    private String etag;

    public ChunkInfo() {}

    public Integer getChunkNumber() { return chunkNumber; }
    public void setChunkNumber(Integer chunkNumber) { this.chunkNumber = chunkNumber; }
    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }
}


