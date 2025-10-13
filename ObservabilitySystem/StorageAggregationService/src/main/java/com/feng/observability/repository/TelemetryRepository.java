package com.feng.observability.repository;

import com.feng.observability.entity.TelemetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Telemetry Repository
 * 
 * Repository interface for telemetry data access.
 */
@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryRecord, Long> {

    /**
     * Find records by event type
     */
    List<TelemetryRecord> findByEventType(String eventType);

    /**
     * Find records by user ID
     */
    List<TelemetryRecord> findByUserId(String userId);

    /**
     * Find records by service name
     */
    List<TelemetryRecord> findByServiceName(String serviceName);

    /**
     * Find records within time range
     */
    List<TelemetryRecord> findByTimestampBetween(Instant startTime, Instant endTime);

    /**
     * Find records by event type and time range
     */
    List<TelemetryRecord> findByEventTypeAndTimestampBetween(String eventType, Instant startTime, Instant endTime);

    /**
     * Find records by user ID and time range
     */
    List<TelemetryRecord> findByUserIdAndTimestampBetween(String userId, Instant startTime, Instant endTime);

    /**
     * Count records by event type
     */
    long countByEventType(String eventType);

    /**
     * Count records by event type and time range
     */
    long countByEventTypeAndTimestampBetween(String eventType, Instant startTime, Instant endTime);

    /**
     * Get distinct event types
     */
    @Query("SELECT DISTINCT t.eventType FROM TelemetryRecord t")
    List<String> findDistinctEventTypes();

    /**
     * Get records for aggregation by time window
     */
    @Query("SELECT t FROM TelemetryRecord t WHERE t.timestamp BETWEEN :startTime AND :endTime ORDER BY t.timestamp ASC")
    List<TelemetryRecord> findRecordsForAggregation(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Get error records by severity
     */
    @Query("SELECT t FROM TelemetryRecord t WHERE t.eventType = 'ERROR' AND t.severity = :severity AND t.timestamp BETWEEN :startTime AND :endTime")
    List<TelemetryRecord> findErrorRecordsBySeverity(@Param("severity") String severity, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Get user activity summary
     */
    @Query("SELECT t.eventType, COUNT(t) FROM TelemetryRecord t WHERE t.userId = :userId AND t.timestamp BETWEEN :startTime AND :endTime GROUP BY t.eventType")
    List<Object[]> getUserActivitySummary(@Param("userId") String userId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Delete old records (for data retention)
     */
    void deleteByTimestampBefore(Instant cutoffTime);
}
