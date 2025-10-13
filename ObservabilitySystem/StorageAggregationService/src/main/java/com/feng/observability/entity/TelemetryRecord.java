package com.feng.observability.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

/**
 * Telemetry Record Entity
 * 
 * JPA entity for storing telemetry data in the database.
 */
@Entity
@Table(name = "telemetry_records", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_service_name", columnList = "serviceName")
})
public class TelemetryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant timestamp;

    @Column
    private String userId;

    @Column
    private String sessionId;

    @Column
    private String serviceName;

    @Column
    private String traceId;

    @Column
    private String spanId;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    @ElementCollection
    @CollectionTable(name = "telemetry_attributes", joinColumns = @JoinColumn(name = "telemetry_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> attributes;

    @Column
    private String severity;

    @Column
    private Double value;

    @Column
    private String currency;

    // Constructors
    public TelemetryRecord() {}

    public TelemetryRecord(String eventType, Instant timestamp) {
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    @Override
    public String toString() {
        return "TelemetryRecord{" +
                "id=" + id +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
