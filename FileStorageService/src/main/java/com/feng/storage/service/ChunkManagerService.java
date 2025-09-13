package com.feng.storage.service;

import com.feng.storage.client.GcsStorageClient;
import com.feng.storage.entity.ChunkedUploadEntity;
import com.feng.storage.entity.FileEntity;
import com.feng.storage.exception.ChunkedUploadNotFoundException;
import com.feng.storage.repository.ChunkedUploadRepository;
import com.feng.storage.repository.FileRepository;
import com.feng.storage.service.api.ChunkUploadResult;
import com.feng.storage.service.api.ChunkedUploadSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkManagerService {

    private final ChunkedUploadRepository chunkedUploadRepository;
    private final FileRepository fileRepository;
    private final GcsStorageClient storageClient;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${file.storage.default-chunk-size:5242880}") // 5MB default
    private int defaultChunkSize;
    
    private static final String CHUNK_SESSION_PREFIX = "chunk_session:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    /**
     * Create a new upload session for chunked upload
     */
    public ChunkedUploadSession createUploadSession(String uploadId, String fileName, long totalSize, 
                                                   int chunkSize, String projectId) {
        int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);
        
        ChunkedUploadSession session = ChunkedUploadSession.builder()
            .uploadId(uploadId)
            .fileName(fileName)
            .totalSize(totalSize)
            .chunkSize(chunkSize)
            .totalChunks(totalChunks)
            .projectId(projectId)
            .uploadedChunks(new BitSet(totalChunks))
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(SESSION_TTL))
            .build();
        
        // Store session in Redis with TTL
        redisTemplate.opsForValue().set(
            CHUNK_SESSION_PREFIX + uploadId, 
            session, 
            SESSION_TTL
        );
        
        return session;
    }
    
    /**
     * Get an existing upload session
     */
    public ChunkedUploadSession getUploadSession(String uploadId) {
        Object session = redisTemplate.opsForValue().get(CHUNK_SESSION_PREFIX + uploadId);
        if (session == null) {
            throw new ChunkedUploadNotFoundException("Upload session not found: " + uploadId);
        }
        return (ChunkedUploadSession) session;
    }
    
    /**
     * Update an existing upload session
     */
    private void updateUploadSession(ChunkedUploadSession session) {
        redisTemplate.opsForValue().set(
            CHUNK_SESSION_PREFIX + session.getUploadId(), 
            session, 
            SESSION_TTL
        );
    }
    
    /**
     * Generate a signed URL for uploading a chunk
     */
    public String generateChunkUploadUrl(String uploadId, int chunkNumber) {
        ChunkedUploadSession session = getUploadSession(uploadId);
        
        // Validate chunk number
        if (chunkNumber < 1 || chunkNumber > session.getTotalChunks()) {
            throw new IllegalArgumentException("Invalid chunk number: " + chunkNumber);
        }
        
        // Generate temporary path for the chunk
        String tempChunkPath = generateTempChunkPath(uploadId, chunkNumber);
        
        // Calculate expiration time
        Duration expiration = Duration.between(Instant.now(), session.getExpiresAt());
        
        // Generate signed URL
        return storageClient.generateSignedUploadUrl(
            storageClient.getDefaultBucket(),
            tempChunkPath, MediaType.MULTIPART_FORM_DATA_VALUE,
            expiration
        );
    }
    
    /**
     * Upload a chunk of a file
     */
    @Transactional
    public ChunkUploadResult uploadChunk(String uploadId, int chunkNumber, MultipartFile chunk) {
        try {
            ChunkedUploadSession session = getUploadSession(uploadId);
            
            // Validate chunk
            validateChunk(session, chunkNumber, chunk);
            
            // Upload chunk to GCS with temporary naming
            String tempChunkPath = generateTempChunkPath(uploadId, chunkNumber);
            storageClient.uploadFile(
                storageClient.getDefaultBucket(), 
                tempChunkPath, 
                chunk.getInputStream(), 
                chunk.getContentType()
            );
            
            // Update session state
            session.getUploadedChunks().set(chunkNumber - 1);
            updateUploadSession(session);
            
            // Check if all chunks are uploaded
            boolean isComplete = session.getUploadedChunks().cardinality() == session.getTotalChunks();
            
            return ChunkUploadResult.builder()
                .chunkNumber(chunkNumber)
                .complete(isComplete)
                .build();
        } catch (IOException e) {
            log.error("Failed to upload chunk {} for upload {}", chunkNumber, uploadId, e);
            throw new RuntimeException("Failed to upload chunk", e);
        }
    }
    
    /**
     * Finalize a chunked upload after all chunks have been uploaded
     */
    @Transactional
    public FileEntity finalizeUpload(String uploadId) {
        ChunkedUploadSession session = getUploadSession(uploadId);
        
        // Verify all chunks are uploaded
        if (session.getUploadedChunks().cardinality() != session.getTotalChunks()) {
            throw new IllegalStateException("Not all chunks have been uploaded");
        }
        
        // Find or create file entity
        FileEntity fileEntity;
        UUID fileId;
        try {
            fileId = UUID.fromString(session.getFileId());
            fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalStateException("File entity not found"));
        } catch (IllegalArgumentException e) {
            // Create new file entity if not exists
            fileEntity = createFileEntity(session);
        }
        
        // Create chunked upload entity
        ChunkedUploadEntity chunkedUpload = createChunkedUploadEntity(session, fileEntity);
        
        // Combine chunks into final file
        String finalObjectKey = fileEntity.getGcsObjectKey();
        combineChunks(uploadId, session.getTotalChunks(), finalObjectKey);
        
        // Update file status
        fileEntity.setUploadStatus(FileEntity.UploadStatus.COMPLETED);
        fileEntity.setUpdatedAt(Instant.now());
        fileRepository.save(fileEntity);
        
        // Clean up temporary chunks
        cleanupChunks(uploadId, session.getTotalChunks());
        
        return fileEntity;
    }
    
    /**
     * Create a file entity for the chunked upload
     */
    private FileEntity createFileEntity(ChunkedUploadSession session) {
        FileEntity fileEntity = FileEntity.builder()
            .filename(session.getFileName())
            .originalFilename(session.getFileName())
            .fileSize(session.getTotalSize())
            .mimeType("application/octet-stream") // Default mime type
            .fileHash("pending") // Will be updated after upload is complete
            .uploadStatus(FileEntity.UploadStatus.UPLOADING)
            .gcsBucket(storageClient.getDefaultBucket())
            .gcsObjectKey("files/" + UUID.randomUUID().toString() + "/" + session.getFileName())
            .projectId(session.getProjectId() != null ? UUID.fromString(session.getProjectId()) : null)
            .createdBy(UUID.randomUUID()) // This should be replaced with actual user ID
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        return fileRepository.save(fileEntity);
    }
    
    /**
     * Create a chunked upload entity
     */
    private ChunkedUploadEntity createChunkedUploadEntity(ChunkedUploadSession session, FileEntity fileEntity) {
        ChunkedUploadEntity chunkedUpload = ChunkedUploadEntity.builder()
            .fileId(fileEntity.getId())
            .uploadId(session.getUploadId())
            .totalChunks(session.getTotalChunks())
            .completedChunks(session.getTotalChunks())
            .chunkSize(session.getChunkSize())
            .createdAt(session.getCreatedAt())
            .expiresAt(session.getExpiresAt())
            .build();
        
        return chunkedUploadRepository.save(chunkedUpload);
    }
    
    /**
     * Validate a chunk before upload
     */
    private void validateChunk(ChunkedUploadSession session, int chunkNumber, MultipartFile chunk) {
        if (chunkNumber < 1 || chunkNumber > session.getTotalChunks()) {
            throw new IllegalArgumentException("Invalid chunk number: " + chunkNumber);
        }
        
        // Check if chunk has already been uploaded
        if (session.getUploadedChunks().get(chunkNumber - 1)) {
            throw new IllegalStateException("Chunk " + chunkNumber + " has already been uploaded");
        }
        
        // Validate chunk size
        long expectedSize = (chunkNumber < session.getTotalChunks()) ? 
                session.getChunkSize() : 
                session.getTotalSize() - (session.getChunkSize() * (session.getTotalChunks() - 1));
        
        if (chunk.getSize() != expectedSize) {
            log.warn("Chunk size mismatch for upload {} chunk {}: expected {}, got {}", 
                    session.getUploadId(), chunkNumber, expectedSize, chunk.getSize());
            // We'll allow this for now, but log a warning
        }
    }
    
    /**
     * Generate a temporary path for a chunk
     */
    private String generateTempChunkPath(String uploadId, int chunkNumber) {
        return "chunks/" + uploadId + "/" + chunkNumber;
    }
    
    /**
     * Combine chunks into a final file
     */
    private void combineChunks(String uploadId, int totalChunks, String finalObjectKey) {
        // In a real implementation, you would use GCS compose API to combine chunks
        // For now, we'll just log that we're doing it
        log.info("Combining {} chunks for upload {} into {}", totalChunks, uploadId, finalObjectKey);
        
        // This would be implemented using GCS compose API in a real implementation
        // For now, we'll just return the final object key
        String result = storageClient.completeMultipartUpload(uploadId, finalObjectKey);
        log.info("Combined chunks into {}", result);
    }
    
    /**
     * Clean up temporary chunks after successful upload
     */
    @Async
    public void cleanupChunks(String uploadId, int totalChunks) {
        // In a real implementation, you would delete the temporary chunks from GCS
        // For now, we'll just log that we're doing it
        log.info("Cleaning up {} chunks for upload {}", totalChunks, uploadId);
        
        // Also remove the session from Redis
        redisTemplate.delete(CHUNK_SESSION_PREFIX + uploadId);
    }
    
    /**
     * Calculate the total number of chunks for a file
     */
    public int calculateTotalChunks(long fileSize) {
        return (int) Math.ceil((double) fileSize / defaultChunkSize);
    }
    
    /**
     * Get the default chunk size
     */
    public int getDefaultChunkSize() {
        return defaultChunkSize;
    }
}