package com.feng.storage.service.api;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileDownloadResponse {

	private String downloadUrl;

	private String filename;

	private Long fileSize;

	private String mimeType;

	private Instant expiresAt;
}


