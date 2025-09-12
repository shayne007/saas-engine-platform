package com.feng.storage.service.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkInfo {
    private Integer chunkNumber;
    private String etag;
}


