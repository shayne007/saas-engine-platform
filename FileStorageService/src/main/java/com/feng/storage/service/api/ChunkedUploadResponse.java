package com.feng.storage.service.api;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkedUploadResponse {
    private String fileId;
    private String uploadId;
    private Instant expiresAt;
}


