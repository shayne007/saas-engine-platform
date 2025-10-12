package com.feng.storage.service.api;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadRequest {

	private String originalFilename;

	private Long fileSize;

	private String mimeType;

	private String fileHash;

	private String userId;

	private String projectId;

	private Map<String, String> tags;

	private Map<String, Object> metadata;

	private Boolean allowDeduplication = Boolean.TRUE;
}


