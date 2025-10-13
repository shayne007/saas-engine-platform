package com.feng.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Login Event Model
 * 
 * Represents a user login event with all relevant telemetry data.
 */
public class LoginEvent {

    @NotBlank(message = "User ID is required")
    @JsonProperty("user_id")
    private String userId;

    @NotBlank(message = "Login method is required")
    @JsonProperty("login_method")
    private String method; // password, oauth, sso, etc.

    @NotNull(message = "Success status is required")
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("failure_reason")
    private String failureReason;

    @JsonProperty("duration_ms")
    private Long durationMs;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public LoginEvent() {}

    public LoginEvent(String userId, String method, Boolean success) {
        this.userId = userId;
        this.method = method;
        this.success = success;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Boolean isSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
        return "LoginEvent{" +
                "userId='" + userId + '\'' +
                ", method='" + method + '\'' +
                ", success=" + success +
                ", failureReason='" + failureReason + '\'' +
                ", durationMs=" + durationMs +
                ", timestamp=" + timestamp +
                '}';
    }
}
