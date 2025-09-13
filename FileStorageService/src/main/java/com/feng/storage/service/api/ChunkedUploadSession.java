package com.feng.storage.service.api;

import java.io.Serializable;
import java.time.Instant;
import java.util.BitSet;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a session for chunked upload, stored in Redis
 */
@Data
@Builder
public class ChunkedUploadSession implements Serializable {

	private static final long serialVersionUID = 1L;

	private String uploadId;

	private String fileId;

	private String fileName;

	private long totalSize;

	private int chunkSize;

	private int totalChunks;

	private String projectId;

	private BitSet uploadedChunks;

	private Instant createdAt;

	private Instant expiresAt;
}