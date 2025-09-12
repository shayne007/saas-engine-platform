package com.feng.storage.sdk;

import com.feng.storage.service.api.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FileStorageSDK {

	private final FileStorageService service;

	private final Duration defaultExpiration;

	public FileStorageSDK(FileStorageService service, Duration defaultExpiration) {
		this.service = service;
		this.defaultExpiration =
				defaultExpiration == null ? Duration.ofHours(1) : defaultExpiration;
	}

	public CompletableFuture<FileUploadResult> uploadFile(File file,
			UploadOptions options) {
		return CompletableFuture.supplyAsync(() -> {
			if (!file.exists() || !file.isFile()) {
				throw new IllegalArgumentException("Invalid file");
			}
			try {
				String hash = sha256(file);
				if (file.length() > options.getChunkThreshold()) {
					return uploadFileInChunks(file, options, hash);
				}
				FileUploadRequest req = FileUploadRequest.builder().build();
				req.setOriginalFilename(file.getName());
				req.setFileSize(file.length());
				req.setMimeType("application/octet-stream");
				req.setFileHash(hash);
				req.setUserId(options.getUserId());
				req.setProjectId(options.getProjectId());
				req.setTags(options.getTags());
				req.setMetadata(options.getMetadata());
				FileUploadResponse resp = service.uploadFile(req);
				return new FileUploadResult(resp.getFileId(), file.getName(),
						file.length(), Instant.now());
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public CompletableFuture<FileDownloadResponse> getDownloadUrl(String fileId) {
		return CompletableFuture.supplyAsync(
				() -> service.getDownloadUrl(fileId, defaultExpiration));
	}

	public FileQuery files() {
		return new FileQuery(service);
	}

	private FileUploadResult uploadFileInChunks(File file, UploadOptions options,
			String hash)
			throws Exception {
		long chunkSize = options.getChunkSize();
		int totalChunks = (int) Math.ceil((double) file.length() / chunkSize);
		ChunkedUploadRequest initReq =
				ChunkedUploadRequest.builder().originalFilename(file.getName())
						.fileSize(file.length())
						.mimeType("application/octet-stream").fileHash(hash)
						.userId(options.getUserId())
						.projectId(options.getProjectId()).totalChunks(totalChunks)
						.chunkSize((int) chunkSize)
						.tags(options.getTags()).metadata(options.getMetadata()).build();
		ChunkedUploadResponse init = service.initiateChunkedUpload(initReq);

		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			for (int i = 0; i < totalChunks; i++) {
				long offset = (long) i * chunkSize;
				int size = (int) Math.min(chunkSize, file.length() - offset);
				byte[] buffer = new byte[size];
				raf.seek(offset);
				raf.readFully(buffer);
				ChunkUploadRequest cur = ChunkUploadRequest.builder().build();
				cur.setUploadId(init.getUploadId());
				cur.setChunkNumber(i + 1);
				service.uploadChunk(cur);
			}
		}

		CompleteChunkedUploadRequest completed = CompleteChunkedUploadRequest.builder()
				.build();
		completed.setUploadId(init.getUploadId());
		completed.setChunks(Collections.emptyList());
		FileUploadResponse complete = service.completeChunkedUpload(completed);

		return new FileUploadResult(complete.getFileId(), file.getName(), file.length(),
				Instant.now());
	}

	private static String sha256(File file) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			byte[] buf = new byte[8192];
			int read;
			while ((read = raf.read(buf)) != -1) {
				digest.update(buf, 0, read);
			}
		}
		byte[] hash = digest.digest();
		StringBuilder sb = new StringBuilder();
		for (byte b : hash) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static class UploadOptions {

		private String userId;

		private String projectId;

		private long chunkThreshold = 5 * 1024 * 1024;

		private long chunkSize = 5 * 1024 * 1024;

		private Map<String, String> tags = new HashMap<>();

		private Map<String, Object> metadata = new HashMap<>();

		public UploadOptions() {
		}

		public String getUserId() {
			return userId;
		}

		public UploadOptions setUserId(String userId) {
			this.userId = userId;
			return this;
		}

		public String getProjectId() {
			return projectId;
		}

		public UploadOptions setProjectId(String projectId) {
			this.projectId = projectId;
			return this;
		}

		public long getChunkThreshold() {
			return chunkThreshold;
		}

		public UploadOptions setChunkThreshold(long chunkThreshold) {
			this.chunkThreshold = chunkThreshold;
			return this;
		}

		public long getChunkSize() {
			return chunkSize;
		}

		public UploadOptions setChunkSize(long chunkSize) {
			this.chunkSize = chunkSize;
			return this;
		}

		public Map<String, String> getTags() {
			return tags;
		}

		public UploadOptions setTags(Map<String, String> tags) {
			this.tags = tags;
			return this;
		}

		public Map<String, Object> getMetadata() {
			return metadata;
		}

		public UploadOptions setMetadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}
	}


	public static class FileUploadResult {

		private String fileId;

		private String filename;

		private long fileSize;

		private Instant uploadedAt;

		public FileUploadResult(String fileId, String filename, long fileSize,
				Instant uploadedAt) {
			this.fileId = fileId;
			this.filename = filename;
			this.fileSize = fileSize;
			this.uploadedAt = uploadedAt;
		}

		public String getFileId() {
			return fileId;
		}

		public String getFilename() {
			return filename;
		}

		public long getFileSize() {
			return fileSize;
		}

		public Instant getUploadedAt() {
			return uploadedAt;
		}
	}


	public static class FileQuery {

		private final FileStorageService service;

		private final FileQueryRequest request = FileQueryRequest.builder().build();

		public FileQuery(FileStorageService service) {
			this.service = service;
		}

		public FileQuery project(String projectId) {
			request.setProjectId(projectId);
			return this;
		}

		public FileQuery createdBy(String userId) {
			request.setCreatedBy(userId);
			return this;
		}

		public FileQuery mimeTypes(String... mimes) {
			request.setMimeTypes(Arrays.asList(mimes));
			return this;
		}

		public CompletableFuture<FileQueryResponse> execute() {
			return CompletableFuture.supplyAsync(() -> service.queryFiles(request));
		}
	}
}


