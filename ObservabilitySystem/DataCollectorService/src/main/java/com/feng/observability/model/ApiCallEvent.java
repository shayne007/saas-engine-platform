package com.feng.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.Map;

/**
 * API Call Event Model
 * 
 * Represents an API call event with performance and error tracking data.
 */
public class ApiCallEvent {

    @NotBlank(message = "Endpoint is required")
    @JsonProperty("endpoint")
    private String endpoint;

    @NotBlank(message = "HTTP method is required")
    @JsonProperty("method")
    private String method;

    @NotNull(message = "Status code is required")
    @Min(value = 100, message = "Status code must be valid HTTP status code")
    @JsonProperty("status_code")
    private Integer statusCode;

    @NotNull(message = "Duration is required")
    @Min(value = 0, message = "Duration must be non-negative")
    @JsonProperty("duration_ms")
    private Long durationMs;

    @JsonProperty("request_size_bytes")
    private Long requestSizeBytes;

    @JsonProperty("response_size_bytes")
    private Long responseSizeBytes;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("span_id")
    private String spanId;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public ApiCallEvent() {}

    public ApiCallEvent(String endpoint, String method, Integer statusCode, Long durationMs) {
        this.endpoint = endpoint;
        this.method = method;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Long getRequestSizeBytes() {
        return requestSizeBytes;
    }

    public void setRequestSizeBytes(Long requestSizeBytes) {
        this.requestSizeBytes = requestSizeBytes;
    }

    public Long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(Long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
        return "ApiCallEvent{" +
                "endpoint='" + endpoint + '\'' +
                ", method='" + method + '\'' +
                ", statusCode=" + statusCode +
                ", durationMs=" + durationMs +
                ", timestamp=" + timestamp +
                '}';
    }
}
