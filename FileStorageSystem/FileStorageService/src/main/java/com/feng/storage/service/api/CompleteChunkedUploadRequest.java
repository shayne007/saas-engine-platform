package com.feng.storage.service.api;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompleteChunkedUploadRequest {

	private String uploadId;

	private List<ChunkInfo> chunks;
}


