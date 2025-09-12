package com.feng.storage.service.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkUploadRequest {
    private String uploadId;
    private Integer chunkNumber;
}


