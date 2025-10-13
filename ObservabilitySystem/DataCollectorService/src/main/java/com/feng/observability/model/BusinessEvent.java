package com.feng.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Business Event Model
 * 
 * Represents custom business events for tracking KPIs and business metrics.
 */
public class BusinessEvent {

    @NotBlank(message = "Event type is required")
    @JsonProperty("event_type")
    private String eventType;

    @NotNull(message = "User ID is required")
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("value")
    private Double value;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("category")
    private String category;

    @JsonProperty("action")
    private String action;

    @JsonProperty("object_id")
    private String objectId;

    @JsonProperty("object_type")
    private String objectType;

    @JsonProperty("funnel_step")
    private String funnelStep;

    @JsonProperty("conversion_goal")
    private String conversionGoal;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public BusinessEvent() {}

    public BusinessEvent(String eventType, String userId) {
        this.eventType = eventType;
        this.userId = userId;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getFunnelStep() {
        return funnelStep;
    }

    public void setFunnelStep(String funnelStep) {
        this.funnelStep = funnelStep;
    }

    public String getConversionGoal() {
        return conversionGoal;
    }

    public void setConversionGoal(String conversionGoal) {
        this.conversionGoal = conversionGoal;
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
        return "BusinessEvent{" +
                "eventType='" + eventType + '\'' +
                ", userId='" + userId + '\'' +
                ", value=" + value +
                ", category='" + category + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
