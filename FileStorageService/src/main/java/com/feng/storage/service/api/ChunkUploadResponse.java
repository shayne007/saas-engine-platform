package com.feng.storage.service.api;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkUploadResponse {

	private String chunkUploadUrl;

	private Instant expiresAt;
	
	@Builder.Default
	private boolean completed = false;
	
	private String fileId;

}


