package com.feng.storage.client;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class GcsStorageClient {
    
    private final Storage storage;
    
    @Value("${gcs.bucket.name:file-storage-bucket}")
    private String defaultBucket;
    
    public String getDefaultBucket() {
        return defaultBucket;
    }
    
    /**
     * Generate signed URL for file upload
     */
    public String generateSignedUploadUrl(String bucket, String objectName, String contentType, Duration expiration) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                    .setContentType(contentType)
                    .build();
            
            return storage.signUrl(blobInfo, expiration.toMillis(), TimeUnit.MILLISECONDS,
                    Storage.SignUrlOption.httpMethod(com.google.cloud.storage.HttpMethod.PUT))
                    .toString();
        } catch (StorageException e) {
            log.error("Failed to generate signed upload URL for {}/{}", bucket, objectName, e);
            throw new RuntimeException("Failed to generate upload URL", e);
        }
    }
    
    /**
     * Generate signed URL for file download
     */
    public String generateSignedDownloadUrl(String bucket, String objectName, Duration expiration) {
        return generateSignedDownloadUrl(bucket, objectName, expiration, null);
    }
    
    /**
     * Generate signed URL for file download with custom headers
     */
    public String generateSignedDownloadUrl(String bucket, String objectName, Duration expiration, Map<String, String> customHeaders) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName).build();
            
            Storage.SignUrlOption[] options = {
                Storage.SignUrlOption.httpMethod(com.google.cloud.storage.HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature()
            };
            
            if (customHeaders != null && !customHeaders.isEmpty()) {
                // Add custom headers for validation
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    options = java.util.Arrays.copyOf(options, options.length + 1);
                    options[options.length - 1] = Storage.SignUrlOption.withExtHeaders(
                        Map.of(entry.getKey(), entry.getValue())
                    );
                }
            }
            
            return storage.signUrl(blobInfo, expiration.toMillis(), TimeUnit.MILLISECONDS, options)
                    .toString();
        } catch (StorageException e) {
            log.error("Failed to generate signed download URL for {}/{}", bucket, objectName, e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }
    
    /**
     * Initiate multipart upload for chunked uploads
     */
    public String initiateMultipartUpload(String bucket, String objectName, String contentType) {
        try {
            // For GCS, we'll use a simple approach with a unique upload ID
            // In a real implementation, you might want to use GCS's compose API
            String uploadId = "upload_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().replace("-", "");
            
            // Create a placeholder blob to track the upload
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                    .setContentType(contentType)
                    .build();
            
            // Store upload metadata (in a real implementation, you'd store this in a database)
            log.info("Initiated multipart upload for {}/{} with ID: {}", bucket, objectName, uploadId);
            
            return uploadId;
        } catch (Exception e) {
            log.error("Failed to initiate multipart upload for {}/{}", bucket, objectName, e);
            throw new RuntimeException("Failed to initiate multipart upload", e);
        }
    }
    
    /**
     * Generate signed URL for chunk upload
     */
    public String generateSignedChunkUploadUrl(String uploadId, Integer chunkNumber, Duration expiration) {
        try {
            // For GCS chunked uploads, we'll use a temporary object name
            String chunkObjectName = "chunks/" + uploadId + "/" + chunkNumber;
            
            BlobInfo blobInfo = BlobInfo.newBuilder(defaultBucket, chunkObjectName)
                    .build();
            
            return storage.signUrl(blobInfo, expiration.toMillis(), TimeUnit.MILLISECONDS,
                    Storage.SignUrlOption.httpMethod(com.google.cloud.storage.HttpMethod.PUT))
                    .toString();
        } catch (StorageException e) {
            log.error("Failed to generate signed chunk upload URL for upload {} chunk {}", uploadId, chunkNumber, e);
            throw new RuntimeException("Failed to generate chunk upload URL", e);
        }
    }
    
    /**
     * Complete multipart upload by composing chunks
     */
    public String completeMultipartUpload(String uploadId, String finalObjectName) {
        try {
            // In a real implementation, you would:
            // 1. List all chunks for this upload
            // 2. Use GCS compose API to combine chunks
            // 3. Delete temporary chunk objects
            // 4. Return the final object name
            
            log.info("Completing multipart upload {} to {}", uploadId, finalObjectName);
            
            // For now, just return the final object name
            return finalObjectName;
        } catch (Exception e) {
            log.error("Failed to complete multipart upload {}", uploadId, e);
            throw new RuntimeException("Failed to complete multipart upload", e);
        }
    }
    
    /**
     * Check if a blob exists
     */
    public boolean blobExists(String bucket, String objectName) {
        try {
            BlobId blobId = BlobId.of(bucket, objectName);
            Blob blob = storage.get(blobId);
            return blob != null && blob.exists();
        } catch (StorageException e) {
            log.warn("Error checking if blob exists: {}/{}", bucket, objectName, e);
            return false;
        }
    }
    
    /**
     * Delete a blob
     */
    public boolean deleteBlob(String bucket, String objectName) {
        try {
            BlobId blobId = BlobId.of(bucket, objectName);
            return storage.delete(blobId);
        } catch (StorageException e) {
            log.error("Failed to delete blob {}/{}", bucket, objectName, e);
            return false;
        }
    }
    
    /**
     * Get blob metadata
     */
    public Blob getBlob(String bucket, String objectName) {
        try {
            BlobId blobId = BlobId.of(bucket, objectName);
            return storage.get(blobId);
        } catch (StorageException e) {
            log.error("Failed to get blob {}/{}", bucket, objectName, e);
            throw new RuntimeException("Failed to get blob", e);
        }
    }
}
