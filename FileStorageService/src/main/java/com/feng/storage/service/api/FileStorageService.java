package com.feng.storage.service.api;

import java.time.Duration;

/**
 * Minimal service interface based on the design document.
 */
public interface FileStorageService {

    FileUploadResponse uploadFile(FileUploadRequest request);

    ChunkedUploadResponse initiateChunkedUpload(ChunkedUploadRequest request);

    ChunkUploadResponse uploadChunk(ChunkUploadRequest request);

    FileUploadResponse completeChunkedUpload(CompleteChunkedUploadRequest request);

    FileDownloadResponse getDownloadUrl(String fileId, Duration expiration);

    FileQueryResponse queryFiles(FileQueryRequest request);

    void deleteFile(String fileId, String userId);

    FileMetadata getFileMetadata(String fileId);
}

