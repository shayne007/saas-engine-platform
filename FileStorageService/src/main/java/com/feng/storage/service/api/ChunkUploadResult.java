package com.feng.storage.service.api;

import lombok.Builder;
import lombok.Data;

/**
 * Result of a chunk upload operation
 */
@Data
@Builder
public class ChunkUploadResult {

    private Integer chunkNumber;
    
    private boolean complete;
}