package com.feng.storage.service.api;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileMetadata {

	private String id;

	private String filename;

	private String originalFilename;

	private Long fileSize;

	private String mimeType;

	private String fileHash;

	private String gcsBucket;

	private String gcsObjectKey;

	private String uploadStatus;

	private String createdBy;

	private String projectId;

	private Map<String, String> tags;

	private Map<String, Object> metadata;

	private Instant createdAt;

	private Instant updatedAt;

	private Instant expiresAt;

}


