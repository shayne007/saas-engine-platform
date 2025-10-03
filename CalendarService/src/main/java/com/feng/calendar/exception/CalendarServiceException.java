package com.feng.calendar.exception;

/**
 * Base exception for Calendar Service
 */
public class CalendarServiceException extends RuntimeException {
    
    public CalendarServiceException(String message) {
        super(message);
    }
    
    public CalendarServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
