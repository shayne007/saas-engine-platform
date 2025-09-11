package com.feng.storage.service.api;

import java.util.List;

public class FileQueryResponse {
    private List<FileMetadata> files;
    private Long total;

    public FileQueryResponse() {}

    public List<FileMetadata> getFiles() { return files; }
    public void setFiles(List<FileMetadata> files) { this.files = files; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
}


