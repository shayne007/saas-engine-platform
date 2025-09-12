package com.feng.storage.service.api;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileQueryRequest {
    private String projectId;
    private List<String> mimeTypes;
    private String createdBy;
    private Instant createdAfter;
    private Instant createdBefore;
    private Integer page;
    private Integer size;
    private String sort;
}


