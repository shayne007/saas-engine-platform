package com.feng.storage.validation;

import com.feng.storage.service.api.FileUploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FileValidator {
    
    @Value("${file-storage.max-file-size:104857600}")
    private long maxFileSize;
    
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain", "text/csv",
        "application/zip", "application/x-zip-compressed",
        "video/mp4", "video/avi", "video/quicktime",
        "audio/mpeg", "audio/wav", "audio/ogg"
    );
    
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final int MAX_FILENAME_LENGTH = 255;
    
    public ValidationResult validateFileUpload(FileUploadRequest request) {
        ValidationResult result = new ValidationResult();
        
        // Validate file size
        if (request.getFileSize() > maxFileSize) {
            result.addError("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        // Validate MIME type
        if (!ALLOWED_MIME_TYPES.contains(request.getMimeType())) {
            result.addError("File type not allowed: " + request.getMimeType());
        }
        
        // Validate filename
        if (!isValidFilename(request.getOriginalFilename())) {
            result.addError("Invalid filename: " + request.getOriginalFilename());
        }
        
        // Validate file hash (SHA-256)
        if (!isValidFileHash(request.getFileHash())) {
            result.addError("Invalid file hash format");
        }
        
        return result;
    }
    
    public ValidationResult validateChunkedUpload(com.feng.storage.service.api.ChunkedUploadRequest request) {
        ValidationResult result = validateFileUpload(FileUploadRequest.builder()
                .originalFilename(request.getOriginalFilename())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .fileHash(request.getFileHash())
                .userId(request.getUserId())
                .projectId(request.getProjectId())
                .build());
        
        // Validate chunk parameters
        if (request.getTotalChunks() < 2 || request.getTotalChunks() > 10000) {
            result.addError("Total chunks must be between 2 and 10000");
        }
        
        if (request.getChunkSize() < 1048576 || request.getChunkSize() > 104857600) {
            result.addError("Chunk size must be between 1MB and 100MB");
        }
        
        return result;
    }
    
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        if (filename.length() > MAX_FILENAME_LENGTH) {
            return false;
        }
        
        // Check for dangerous characters
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        
        // Check for valid characters
        return FILENAME_PATTERN.matcher(filename).matches();
    }
    
    private boolean isValidFileHash(String hash) {
        if (hash == null || hash.length() != 64) {
            return false;
        }
        
        // Check if it's a valid hexadecimal string
        return hash.matches("^[a-fA-F0-9]+$");
    }
    
    public static class ValidationResult {
        private boolean valid = true;
        private List<String> errors = new java.util.ArrayList<>();
        
        public boolean isValid() {
            return valid && errors.isEmpty();
        }
        
        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
