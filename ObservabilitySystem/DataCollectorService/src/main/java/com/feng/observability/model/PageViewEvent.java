package com.feng.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Page View Event Model
 * 
 * Represents a page view event with navigation and performance data.
 */
public class PageViewEvent {

    @NotBlank(message = "User ID is required")
    @JsonProperty("user_id")
    private String userId;

    @NotBlank(message = "Page name is required")
    @JsonProperty("page_name")
    private String pageName;

    @NotBlank(message = "Page URL is required")
    @JsonProperty("page_url")
    private String pageUrl;

    @JsonProperty("referrer")
    private String referrer;

    @JsonProperty("load_time_ms")
    private Long loadTimeMs;

    @JsonProperty("viewport_width")
    private Integer viewportWidth;

    @JsonProperty("viewport_height")
    private Integer viewportHeight;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public PageViewEvent() {}

    public PageViewEvent(String userId, String pageName, String pageUrl) {
        this.userId = userId;
        this.pageName = pageName;
        this.pageUrl = pageUrl;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public Long getLoadTimeMs() {
        return loadTimeMs;
    }

    public void setLoadTimeMs(Long loadTimeMs) {
        this.loadTimeMs = loadTimeMs;
    }

    public Integer getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(Integer viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public Integer getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(Integer viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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
        return "PageViewEvent{" +
                "userId='" + userId + '\'' +
                ", pageName='" + pageName + '\'' +
                ", pageUrl='" + pageUrl + '\'' +
                ", loadTimeMs=" + loadTimeMs +
                ", timestamp=" + timestamp +
                '}';
    }
}
