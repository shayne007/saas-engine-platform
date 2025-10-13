package com.feng.observability.service;

import com.feng.observability.entity.TelemetryRecord;
import com.feng.observability.repository.TelemetryRepository;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.common.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregation Service
 * 
 * Service for aggregating telemetry data and generating metrics.
 */
@Service
public class AggregationService {

    private static final Logger logger = LoggerFactory.getLogger(AggregationService.class);

    @Autowired
    private TelemetryRepository telemetryRepository;

    private final Meter meter;
    
    // Aggregated metrics
    private LongCounter totalEventsProcessed;
    private LongCounter aggregatedMetricsGenerated;
    private DoubleGauge loginSuccessRate;
    private DoubleGauge averagePageLoadTime;
    private DoubleGauge apiErrorRate;
    private DoubleGauge activeUsersCount;

    // In-memory aggregation caches
    private final Map<String, Long> eventTypeCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> userActivityCounts = new ConcurrentHashMap<>();
    private final Map<String, Double> performanceMetrics = new ConcurrentHashMap<>();

    public AggregationService() {
        this.meter = io.opentelemetry.api.GlobalOpenTelemetry.getMeter("aggregation-service");
    }

    @PostConstruct
    public void initializeMetrics() {
        totalEventsProcessed = meter.counterBuilder("aggregation.events.processed")
                .setDescription("Total events processed by aggregation service")
                .setUnit("1")
                .build();

        aggregatedMetricsGenerated = meter.counterBuilder("aggregation.metrics.generated")
                .setDescription("Total aggregated metrics generated")
                .setUnit("1")
                .build();

        loginSuccessRate = meter.gaugeBuilder("aggregation.login.success_rate")
                .setDescription("Login success rate percentage")
                .setUnit("%")
                .buildWithCallback(measurement -> {
                    measurement.record(calculateLoginSuccessRate(), Attributes.empty());
                });

        averagePageLoadTime = meter.gaugeBuilder("aggregation.page.load_time_avg")
                .setDescription("Average page load time")
                .setUnit("ms")
                .buildWithCallback(measurement -> {
                    measurement.record(calculateAveragePageLoadTime(), Attributes.empty());
                });

        apiErrorRate = meter.gaugeBuilder("aggregation.api.error_rate")
                .setDescription("API error rate percentage")
                .setUnit("%")
                .buildWithCallback(measurement -> {
                    measurement.record(calculateApiErrorRate(), Attributes.empty());
                });

        activeUsersCount = meter.gaugeBuilder("aggregation.users.active_count")
                .setDescription("Number of active users")
                .setUnit("1")
                .buildWithCallback(measurement -> {
                    measurement.record(calculateActiveUsersCount(), Attributes.empty());
                });

        logger.info("Aggregation metrics initialized successfully");
    }

    /**
     * Process telemetry record for aggregation
     */
    public void processTelemetryRecord(TelemetryRecord record) {
        try {
            // Update event type counts
            eventTypeCounts.merge(record.getEventType(), 1L, Long::sum);

            // Update user activity counts
            if (record.getUserId() != null) {
                userActivityCounts.merge(record.getUserId(), 1L, Long::sum);
            }

            // Update performance metrics based on event type
            updatePerformanceMetrics(record);

            // Record metrics
            totalEventsProcessed.add(1, Attributes.of(
                    "event.type", record.getEventType(),
                    "service.name", record.getServiceName() != null ? record.getServiceName() : "unknown"
            ));

            logger.debug("Processed telemetry record: {} for event type: {}", record.getId(), record.getEventType());

        } catch (Exception e) {
            logger.error("Error processing telemetry record: {}", record.getId(), e);
        }
    }

    /**
     * Scheduled aggregation task - runs every minute
     */
    @Scheduled(fixedRate = 60000)
    public void performScheduledAggregation() {
        try {
            Instant now = Instant.now();
            Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

            logger.info("Starting scheduled aggregation for time range: {} to {}", oneHourAgo, now);

            // Aggregate login events
            aggregateLoginEvents(oneHourAgo, now);

            // Aggregate page view events
            aggregatePageViewEvents(oneHourAgo, now);

            // Aggregate API call events
            aggregateApiCallEvents(oneHourAgo, now);

            // Aggregate error events
            aggregateErrorEvents(oneHourAgo, now);

            // Aggregate business events
            aggregateBusinessEvents(oneHourAgo, now);

            // Generate aggregated metrics
            generateAggregatedMetrics();

            logger.info("Completed scheduled aggregation");

        } catch (Exception e) {
            logger.error("Error during scheduled aggregation", e);
        }
    }

    /**
     * Clean up old data - runs daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void cleanupOldData() {
        try {
            Instant cutoffTime = Instant.now().minus(30, ChronoUnit.DAYS);
            long deletedCount = telemetryRepository.countByTimestampBetween(Instant.EPOCH, cutoffTime);
            
            telemetryRepository.deleteByTimestampBefore(cutoffTime);
            
            logger.info("Cleaned up {} old telemetry records older than {}", deletedCount, cutoffTime);

        } catch (Exception e) {
            logger.error("Error during data cleanup", e);
        }
    }

    /**
     * Aggregate login events
     */
    private void aggregateLoginEvents(Instant startTime, Instant endTime) {
        List<TelemetryRecord> loginEvents = telemetryRepository.findByEventTypeAndTimestampBetween("LOGIN", startTime, endTime);
        
        long totalLogins = loginEvents.size();
        long successfulLogins = loginEvents.stream()
                .mapToLong(record -> "success".equals(record.getAttributes().get("status")) ? 1 : 0)
                .sum();

        double successRate = totalLogins > 0 ? (double) successfulLogins / totalLogins * 100 : 0;

        performanceMetrics.put("login.success_rate", successRate);
        performanceMetrics.put("login.total_count", (double) totalLogins);

        logger.debug("Login aggregation: {} total, {} successful, {:.2f}% success rate", 
                totalLogins, successfulLogins, successRate);
    }

    /**
     * Aggregate page view events
     */
    private void aggregatePageViewEvents(Instant startTime, Instant endTime) {
        List<TelemetryRecord> pageViewEvents = telemetryRepository.findByEventTypeAndTimestampBetween("PAGE_VIEW", startTime, endTime);
        
        double averageLoadTime = pageViewEvents.stream()
                .filter(record -> record.getAttributes().containsKey("load_time_ms"))
                .mapToDouble(record -> Double.parseDouble(record.getAttributes().get("load_time_ms")))
                .average()
                .orElse(0.0);

        performanceMetrics.put("page.load_time_avg", averageLoadTime);
        performanceMetrics.put("page.view_count", (double) pageViewEvents.size());

        logger.debug("Page view aggregation: {} views, {:.2f}ms average load time", 
                pageViewEvents.size(), averageLoadTime);
    }

    /**
     * Aggregate API call events
     */
    private void aggregateApiCallEvents(Instant startTime, Instant endTime) {
        List<TelemetryRecord> apiCallEvents = telemetryRepository.findByEventTypeAndTimestampBetween("API_CALL", startTime, endTime);
        
        long totalCalls = apiCallEvents.size();
        long errorCalls = apiCallEvents.stream()
                .mapToLong(record -> {
                    String statusCategory = record.getAttributes().get("status_category");
                    return "error".equals(statusCategory) ? 1 : 0;
                })
                .sum();

        double errorRate = totalCalls > 0 ? (double) errorCalls / totalCalls * 100 : 0;

        performanceMetrics.put("api.error_rate", errorRate);
        performanceMetrics.put("api.total_calls", (double) totalCalls);

        logger.debug("API call aggregation: {} total, {} errors, {:.2f}% error rate", 
                totalCalls, errorCalls, errorRate);
    }

    /**
     * Aggregate error events
     */
    private void aggregateErrorEvents(Instant startTime, Instant endTime) {
        List<TelemetryRecord> errorEvents = telemetryRepository.findByEventTypeAndTimestampBetween("ERROR", startTime, endTime);
        
        Map<String, Long> errorTypeCounts = errorEvents.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        record -> record.getAttributes().get("error_type"),
                        java.util.stream.Collectors.counting()
                ));

        performanceMetrics.put("error.total_count", (double) errorEvents.size());

        logger.debug("Error aggregation: {} total errors", errorEvents.size());
        errorTypeCounts.forEach((type, count) -> 
                logger.debug("Error type {}: {} occurrences", type, count));
    }

    /**
     * Aggregate business events
     */
    private void aggregateBusinessEvents(Instant startTime, Instant endTime) {
        List<TelemetryRecord> businessEvents = telemetryRepository.findByEventTypeAndTimestampBetween("BUSINESS", startTime, endTime);
        
        double totalValue = businessEvents.stream()
                .filter(record -> record.getValue() != null)
                .mapToDouble(TelemetryRecord::getValue)
                .sum();

        performanceMetrics.put("business.total_value", totalValue);
        performanceMetrics.put("business.event_count", (double) businessEvents.size());

        logger.debug("Business event aggregation: {} events, total value: {:.2f}", 
                businessEvents.size(), totalValue);
    }

    /**
     * Generate aggregated metrics
     */
    private void generateAggregatedMetrics() {
        aggregatedMetricsGenerated.add(1, Attributes.of(
                "aggregation.type", "scheduled"
        ));
    }

    /**
     * Update performance metrics for a specific record
     */
    private void updatePerformanceMetrics(TelemetryRecord record) {
        String eventType = record.getEventType();
        
        switch (eventType) {
            case "LOGIN":
                if (record.getAttributes().containsKey("duration_ms")) {
                    double duration = Double.parseDouble(record.getAttributes().get("duration_ms"));
                    performanceMetrics.merge("login.duration_avg", duration, (a, b) -> (a + b) / 2);
                }
                break;
                
            case "PAGE_VIEW":
                if (record.getAttributes().containsKey("load_time_ms")) {
                    double loadTime = Double.parseDouble(record.getAttributes().get("load_time_ms"));
                    performanceMetrics.merge("page.load_time_avg", loadTime, (a, b) -> (a + b) / 2);
                }
                break;
                
            case "API_CALL":
                if (record.getAttributes().containsKey("duration_ms")) {
                    double duration = Double.parseDouble(record.getAttributes().get("duration_ms"));
                    performanceMetrics.merge("api.duration_avg", duration, (a, b) -> (a + b) / 2);
                }
                break;
        }
    }

    /**
     * Calculate login success rate
     */
    private double calculateLoginSuccessRate() {
        return performanceMetrics.getOrDefault("login.success_rate", 0.0);
    }

    /**
     * Calculate average page load time
     */
    private double calculateAveragePageLoadTime() {
        return performanceMetrics.getOrDefault("page.load_time_avg", 0.0);
    }

    /**
     * Calculate API error rate
     */
    private double calculateApiErrorRate() {
        return performanceMetrics.getOrDefault("api.error_rate", 0.0);
    }

    /**
     * Calculate active users count
     */
    private double calculateActiveUsersCount() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        return telemetryRepository.findByTimestampBetween(oneHourAgo, Instant.now())
                .stream()
                .map(TelemetryRecord::getUserId)
                .filter(userId -> userId != null)
                .distinct()
                .count();
    }

    /**
     * Get aggregation summary
     */
    public Map<String, Object> getAggregationSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("event_type_counts", new java.util.HashMap<>(eventTypeCounts));
        summary.put("performance_metrics", new java.util.HashMap<>(performanceMetrics));
        summary.put("active_users_count", calculateActiveUsersCount());
        summary.put("last_updated", Instant.now());
        return summary;
    }
}
