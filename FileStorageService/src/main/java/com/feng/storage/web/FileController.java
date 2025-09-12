package com.feng.storage.web;

import com.feng.storage.service.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@Validated
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@Valid @RequestBody FileUploadRequest request) {
        return ResponseEntity.ok(fileStorageService.uploadFile(request));
    }

    @PostMapping("/upload/chunked")
    public ResponseEntity<ChunkedUploadResponse> initiateChunkedUpload(@Valid @RequestBody ChunkedUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileStorageService.initiateChunkedUpload(request));
    }

    @PostMapping("/upload/chunked/{uploadId}/chunks/{chunkNumber}")
    public ResponseEntity<ChunkUploadResponse> getChunkUploadUrl(@PathVariable String uploadId, @PathVariable Integer chunkNumber) {
        ChunkUploadRequest req = ChunkUploadRequest.builder()
            .uploadId(uploadId)
            .chunkNumber(chunkNumber)
            .build();
        return ResponseEntity.ok(fileStorageService.uploadChunk(req));
    }

    @PostMapping("/upload/chunked/{uploadId}/complete")
    public ResponseEntity<FileUploadResponse> completeChunkedUpload(@PathVariable String uploadId, @Valid @RequestBody CompleteChunkedUploadRequest request) {
        request.setUploadId(uploadId);
        return ResponseEntity.ok(fileStorageService.completeChunkedUpload(request));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<FileDownloadResponse> getDownloadUrl(@PathVariable String fileId, @RequestParam(defaultValue = "3600") Long expirationSeconds) {
        Duration expiration = Duration.ofSeconds(expirationSeconds);
        return ResponseEntity.ok(fileStorageService.getDownloadUrl(fileId, expiration));
    }

    @GetMapping
    public ResponseEntity<FileQueryResponse> queryFiles(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) List<String> mimeTypes,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAfter,
            @RequestParam(required = false) Instant createdBefore,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        FileQueryRequest req = FileQueryRequest.builder()
            .projectId(projectId)
            .mimeTypes(mimeTypes)
            .createdBy(createdBy)
            .createdAfter(createdAfter)
            .createdBefore(createdBefore)
            .page(page)
            .size(size)
            .sort(sort)
            .build();
        return ResponseEntity.ok(fileStorageService.queryFiles(req));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable String fileId) {
        return ResponseEntity.ok(fileStorageService.getFileMetadata(fileId));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId, @RequestHeader(value = "X-User-Id", required = false) String userId) {
        fileStorageService.deleteFile(fileId, userId == null ? "system" : userId);
        return ResponseEntity.noContent().build();
    }
}


