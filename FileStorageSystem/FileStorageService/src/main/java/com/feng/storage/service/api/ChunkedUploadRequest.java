package com.feng.storage.service.api;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkedUploadRequest {

	private String originalFilename;

	private Long fileSize;

	private String mimeType;

	private String fileHash;

	private String userId;

	private String projectId;

	private Integer totalChunks;

	private Integer chunkSize;

	private Map<String, String> tags;

	private Map<String, Object> metadata;
}


