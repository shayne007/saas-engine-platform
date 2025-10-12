package com.feng.storage.service;

import com.feng.storage.client.GcsStorageClient;
import com.feng.storage.entity.FileEntity;
import com.feng.storage.repository.FileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileRepository fileRepository;
    private final GcsStorageClient storageClient;
    
    /**
     * Upload a file directly (non-chunked)
     */
    @Transactional
    public FileEntity uploadFile(MultipartFile file, String projectId) {
        try {
            // Create file entity
            FileEntity fileEntity = FileEntity.builder()
                .filename(file.getOriginalFilename())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .fileHash("pending") // Would be calculated in a real implementation
                .uploadStatus(FileEntity.UploadStatus.UPLOADING)
                .gcsBucket(storageClient.getDefaultBucket())
                .gcsObjectKey("files/" + UUID.randomUUID().toString() + "/" + file.getOriginalFilename())
                .projectId(projectId != null ? UUID.fromString(projectId) : null)
                .createdBy(UUID.randomUUID()) // This should be replaced with actual user ID
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            fileRepository.save(fileEntity);
            
            // Upload to GCS
            storageClient.uploadFile(
                fileEntity.getGcsBucket(),
                fileEntity.getGcsObjectKey(),
                file.getInputStream(),
                file.getContentType()
            );
            
            // Update status
            fileEntity.setUploadStatus(FileEntity.UploadStatus.COMPLETED);
            fileEntity.setUpdatedAt(Instant.now());
            fileRepository.save(fileEntity);
            
            return fileEntity;
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }
    
    /**
     * Get file by ID
     */
    public FileEntity getFile(UUID fileId) {
        return fileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
    }
    
    /**
     * Generate a signed URL for downloading a file
     */
    public String generateDownloadUrl(UUID fileId, Duration expiration) {
        FileEntity fileEntity = getFile(fileId);
        
        return storageClient.generateSignedDownloadUrl(
            fileEntity.getGcsBucket(),
            fileEntity.getGcsObjectKey(),
            expiration
        );
    }
    
    /**
     * Delete a file
     */
    @Transactional
    public void deleteFile(UUID fileId) {
        FileEntity fileEntity = getFile(fileId);
        
        // Delete from GCS
        storageClient.deleteBlob(fileEntity.getGcsBucket(), fileEntity.getGcsObjectKey());
        
        // Delete from database
        fileRepository.delete(fileEntity);
    }
}