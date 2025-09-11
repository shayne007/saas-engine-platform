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
        FileMetadata meta = new FileMetadata();
        meta.setId(id);
        meta.setFilename(request.getOriginalFilename());
        meta.setOriginalFilename(request.getOriginalFilename());
        meta.setFileSize(request.getFileSize());
        meta.setMimeType(request.getMimeType());
        meta.setFileHash(request.getFileHash());
        meta.setGcsBucket("in-memory");
        meta.setGcsObjectKey("mem://" + id);
        meta.setUploadStatus("COMPLETED");
        meta.setCreatedBy(request.getUserId());
        meta.setProjectId(request.getProjectId());
        meta.setTags(request.getTags());
        meta.setMetadata(request.getMetadata());
        meta.setCreatedAt(Instant.now());
        meta.setUpdatedAt(Instant.now());
        files.put(id, meta);
        FileUploadResponse resp = new FileUploadResponse();
        resp.setFileId(id);
        resp.setUploadUrl(null);
        resp.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        resp.setIsDuplicate(Boolean.FALSE);
        return resp;
    }

    @Override
    public ChunkedUploadResponse initiateChunkedUpload(ChunkedUploadRequest request) {
        String fileId = UUID.randomUUID().toString();
        String uploadId = UUID.randomUUID().toString();
        FileMetadata meta = new FileMetadata();
        meta.setId(fileId);
        meta.setFilename(request.getOriginalFilename());
        meta.setOriginalFilename(request.getOriginalFilename());
        meta.setFileSize(request.getFileSize());
        meta.setMimeType(request.getMimeType());
        meta.setFileHash(request.getFileHash());
        meta.setGcsBucket("in-memory");
        meta.setGcsObjectKey("mem://" + fileId);
        meta.setUploadStatus("UPLOADING");
        meta.setCreatedBy(request.getUserId());
        meta.setProjectId(request.getProjectId());
        meta.setTags(request.getTags());
        meta.setMetadata(request.getMetadata());
        meta.setCreatedAt(Instant.now());
        meta.setUpdatedAt(Instant.now());
        files.put(fileId, meta);
        uploadIdToFileId.put(uploadId, fileId);
        uploadChunks.put(uploadId, new ConcurrentHashMap<>());
        ChunkedUploadResponse r = new ChunkedUploadResponse();
        r.setFileId(fileId);
        r.setUploadId(uploadId);
        r.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
        return r;
    }

    @Override
    public ChunkUploadResponse uploadChunk(ChunkUploadRequest request) {
        uploadChunks.computeIfAbsent(request.getUploadId(), k -> new ConcurrentHashMap<>())
                .put(request.getChunkNumber(), "etag-" + request.getChunkNumber());
        ChunkUploadResponse r = new ChunkUploadResponse();
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
        FileUploadResponse r = new FileUploadResponse();
        r.setFileId(fileId);
        r.setUploadUrl(null);
        r.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        r.setIsDuplicate(Boolean.FALSE);
        return r;
    }

    @Override
    public FileDownloadResponse getDownloadUrl(String fileId, Duration expiration) {
        FileMetadata meta = files.get(fileId);
        if (meta == null) throw new IllegalArgumentException("File not found");
        FileDownloadResponse r = new FileDownloadResponse();
        r.setDownloadUrl("mem://download/" + fileId);
        r.setFilename(meta.getOriginalFilename());
        r.setFileSize(meta.getFileSize());
        r.setMimeType(meta.getMimeType());
        r.setExpiresAt(Instant.now().plus(expiration));
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
        FileQueryResponse r = new FileQueryResponse();
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


