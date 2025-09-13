package com.feng.storage.service.api;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponse {

	private String fileId;
	private String uploadId;

	private String uploadUrl;

	private Instant expiresAt;

	private Boolean isDuplicate;
	
	private String error;
	
	private Boolean chunkedUpload;

	private Integer totalChunks;

	private Integer chunkSize;
}


