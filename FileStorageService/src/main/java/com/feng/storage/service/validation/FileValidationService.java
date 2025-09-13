package com.feng.storage.service.validation;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.feng.storage.exception.FileValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for validating file uploads according to security and business rules
 */
@Service
@Slf4j
public class FileValidationService {

    @Value("${file.validation.max-size:104857600}") // 100MB default
    private long maxFileSize;
    
    @Value("${file.validation.allowed-mime-types:image/jpeg,image/png,image/gif,application/pdf,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}")
    private String allowedMimeTypesString;
    
    @Value("${file.validation.allowed-extensions:.jpg,.jpeg,.png,.gif,.pdf,.txt,.doc,.docx,.xls,.xlsx}")
    private String allowedExtensionsString;
    
    private List<String> getAllowedMimeTypes() {
        return Arrays.asList(allowedMimeTypesString.split(","));
    }
    
    private List<String> getAllowedExtensions() {
        return Arrays.asList(allowedExtensionsString.split(","));
    }
    
    /**
     * Validate a file upload
     * @param file The MultipartFile to validate
     * @throws FileValidationException if validation fails
     */
    public void validateFile(MultipartFile file) throws FileValidationException {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty or null");
        }
        
        validateFileSize(file);
        validateContentType(file);
        validateFileExtension(file);
        
        // Additional validations could be added here:
        // - Malware scanning
        // - File signature validation
        // - Content validation
        
        log.info("File validation passed for file: {}", file.getOriginalFilename());
    }
    
    /**
     * Validate file size
     */
    private void validateFileSize(MultipartFile file) throws FileValidationException {
        if (file.getSize() > maxFileSize) {
            throw new FileValidationException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }
    }
    
    /**
     * Validate file content type
     */
    private void validateContentType(MultipartFile file) throws FileValidationException {
        String contentType = file.getContentType();
        if (contentType == null || !getAllowedMimeTypes().contains(contentType)) {
            throw new FileValidationException("File type not allowed: " + contentType);
        }
    }
    
    /**
     * Validate file extension
     */
    private void validateFileExtension(MultipartFile file) throws FileValidationException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new FileValidationException("Filename is null");
        }
        
        boolean validExtension = false;
        for (String ext : getAllowedExtensions()) {
            if (filename.toLowerCase().endsWith(ext.toLowerCase())) {
                validExtension = true;
                break;
            }
        }
        
        if (!validExtension) {
            throw new FileValidationException("File extension not allowed");
        }
    }
}