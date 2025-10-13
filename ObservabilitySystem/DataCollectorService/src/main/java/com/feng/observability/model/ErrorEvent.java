package com.feng.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Error Event Model
 * 
 * Represents an error event with detailed error information and context.
 */
public class ErrorEvent {

    @NotBlank(message = "Error type is required")
    @JsonProperty("error_type")
    private String errorType;

    @NotBlank(message = "Error message is required")
    @JsonProperty("message")
    private String message;

    @NotNull(message = "Severity is required")
    @JsonProperty("severity")
    private ErrorSeverity severity;

    @JsonProperty("stack_trace")
    private String stackTrace;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("span_id")
    private String spanId;

    @JsonProperty("service_name")
    private String serviceName;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public ErrorEvent() {}

    public ErrorEvent(String errorType, String message, ErrorSeverity severity) {
        this.errorType = errorType;
        this.message = message;
        this.severity = severity;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ErrorSeverity severity) {
        this.severity = severity;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "ErrorEvent{" +
                "errorType='" + errorType + '\'' +
                ", message='" + message + '\'' +
                ", severity=" + severity +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Error Severity Enumeration
     */
    public enum ErrorSeverity {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
        CRITICAL("critical");

        private final String value;

        ErrorSeverity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
