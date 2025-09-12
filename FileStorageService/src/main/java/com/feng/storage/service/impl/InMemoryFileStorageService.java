package com.feng.storage.service.impl;

import com.feng.storage.service.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory implementation to make the SDK usable for now.
 */
public class InMemoryFileStorageService implements FileStorageService {

    private final Map<String, FileMetadata> files = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, String>> uploadChunks = new ConcurrentHashMap<>();
    private final Map<String, String> uploadIdToFileId = new ConcurrentHashMap<>();

    @Override
    public FileUploadResponse uploadFile(FileUploadRequest request) {
        String id = UUID.randomUUID().toString();
        FileMetadata meta = FileMetadata.builder()
            .id(id)
            .filename(request.getOriginalFilename())
            .originalFilename(request.getOriginalFilename())
            .fileSize(request.getFileSize())
            .mimeType(request.getMimeType())
            .fileHash(request.getFileHash())
            .gcsBucket("in-memory")
            .gcsObjectKey("mem://" + id)
            .uploadStatus("COMPLETED")
            .createdBy(request.getUserId())
            .projectId(request.getProjectId())
            .tags(request.getTags())
            .metadata(request.getMetadata())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        files.put(id, meta);
        FileUploadResponse resp = FileUploadResponse.builder()
            .fileId(id)
            .uploadUrl(null)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
            .isDuplicate(Boolean.FALSE)
            .build();
        return resp;
    }

    @Override
    public ChunkedUploadResponse initiateChunkedUpload(ChunkedUploadRequest request) {
        String fileId = UUID.randomUUID().toString();
        String uploadId = UUID.randomUUID().toString();
        FileMetadata meta = FileMetadata.builder()
            .id(fileId)
            .filename(request.getOriginalFilename())
            .originalFilename(request.getOriginalFilename())
            .fileSize(request.getFileSize())
            .mimeType(request.getMimeType())
            .fileHash(request.getFileHash())
            .gcsBucket("in-memory")
            .gcsObjectKey("mem://" + fileId)
            .uploadStatus("UPLOADING")
            .createdBy(request.getUserId())
            .projectId(request.getProjectId())
            .tags(request.getTags())
            .metadata(request.getMetadata())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        files.put(fileId, meta);
        uploadIdToFileId.put(uploadId, fileId);
        uploadChunks.put(uploadId, new ConcurrentHashMap<>());
        ChunkedUploadResponse r = ChunkedUploadResponse.builder()
            .fileId(fileId)
            .uploadId(uploadId)
            .expiresAt(Instant.now().plus(Duration.ofHours(1)))
            .build();
        return r;
    }

    @Override
    public ChunkUploadResponse uploadChunk(ChunkUploadRequest request) {
        uploadChunks.computeIfAbsent(request.getUploadId(), k -> new ConcurrentHashMap<>())
                .put(request.getChunkNumber(), "etag-" + request.getChunkNumber());
        ChunkUploadResponse r = ChunkUploadResponse.builder().build();
        r.setChunkUploadUrl("mem://upload/" + request.getUploadId() + "/" + request.getChunkNumber());
        r.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        return r;
    }

    @Override
    public FileUploadResponse completeChunkedUpload(CompleteChunkedUploadRequest request) {
        String fileId = uploadIdToFileId.get(request.getUploadId());
        if (fileId == null) throw new IllegalArgumentException("Invalid uploadId");
        FileMetadata meta = files.get(fileId);
        if (meta == null) throw new IllegalStateException("File not found");
        meta.setUploadStatus("COMPLETED");
        meta.setUpdatedAt(Instant.now());
        FileUploadResponse r = FileUploadResponse.builder()
            .fileId(fileId)
            .uploadUrl(null)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
            .isDuplicate(Boolean.FALSE)
            .build();
        return r;
    }

    @Override
    public FileDownloadResponse getDownloadUrl(String fileId, Duration expiration) {
        FileMetadata meta = files.get(fileId);
        if (meta == null) throw new IllegalArgumentException("File not found");
        FileDownloadResponse r = FileDownloadResponse.builder()
            .downloadUrl("mem://download/" + fileId)
            .filename(meta.getOriginalFilename())
            .fileSize(meta.getFileSize())
            .mimeType(meta.getMimeType())
            .expiresAt(Instant.now().plus(expiration))
            .build();
        return r;
    }

    @Override
    public FileQueryResponse queryFiles(FileQueryRequest request) {
        List<FileMetadata> all = new ArrayList<>(files.values());
        List<FileMetadata> filtered = all.stream()
                .filter(f -> request.getProjectId() == null || Objects.equals(request.getProjectId(), f.getProjectId()))
                .filter(f -> request.getCreatedBy() == null || Objects.equals(request.getCreatedBy(), f.getCreatedBy()))
                .filter(f -> request.getMimeTypes() == null || request.getMimeTypes().isEmpty() || request.getMimeTypes().contains(f.getMimeType()))
                .collect(Collectors.toList());
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        int size = Optional.ofNullable(request.getSize()).orElse(20);
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        FileQueryResponse r = FileQueryResponse.builder().build();
        r.setFiles(filtered.subList(from, to));
        r.setTotal((long) filtered.size());
        return r;
    }

    @Override
    public void deleteFile(String fileId, String userId) {
        files.remove(fileId);
    }

    @Override
    public FileMetadata getFileMetadata(String fileId) {
        FileMetadata meta = files.get(fileId);
        if (meta == null) throw new IllegalArgumentException("File not found");
        return meta;
    }
}


