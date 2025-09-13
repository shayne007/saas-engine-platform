package com.feng.storage.service.api;

import java.time.Duration;

import org.springframework.web.multipart.MultipartFile;

/**
 * Minimal service interface based on the design document.
 */
public interface FileStorageService {

	FileUploadResponse uploadFile(FileUploadRequest request);

	FileUploadResponse uploadFile(MultipartFile file, FileUploadRequest request);
	
	long getMaxSingleUploadSize();
	
	int getDefaultChunkSize();

	ChunkedUploadResponse initiateChunkedUpload(ChunkedUploadRequest request);

	ChunkUploadResponse uploadChunk(ChunkUploadRequest request);

	FileUploadResponse completeChunkedUpload(CompleteChunkedUploadRequest request);

	FileDownloadResponse getDownloadUrl(String fileId, Duration expiration);

	FileQueryResponse queryFiles(FileQueryRequest request);

	void deleteFile(String fileId, String userId);

	FileMetadata getFileMetadata(String fileId);
}

