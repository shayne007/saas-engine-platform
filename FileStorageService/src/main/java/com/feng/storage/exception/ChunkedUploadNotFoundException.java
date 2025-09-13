package com.feng.storage.exception;

/**
 * Exception thrown when a chunked upload is not found
 */
public class ChunkedUploadNotFoundException extends RuntimeException {

    public ChunkedUploadNotFoundException(String message) {
        super(message);
    }

    public ChunkedUploadNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}