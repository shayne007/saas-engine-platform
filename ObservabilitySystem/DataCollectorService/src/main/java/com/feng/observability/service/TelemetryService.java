package com.feng.observability.service;

import com.feng.observability.model.*;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

/**
 * Telemetry Service
 * 
 * Service for recording and processing telemetry data using OpenTelemetry.
 */
@Service
public class TelemetryService {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryService.class);

    private final Tracer tracer;
    private final Meter meter;

    // Login metrics
    private LongCounter loginAttemptsCounter;
    private LongHistogram loginDurationHistogram;

    // Page view metrics
    private LongCounter pageViewCounter;
    private LongHistogram pageLoadTimeHistogram;

    // API call metrics
    private LongCounter apiCallCounter;
    private LongHistogram apiDurationHistogram;

    // Error metrics
    private LongCounter errorCounter;

    // Business metrics
    private LongCounter businessEventCounter;

    public TelemetryService() {
        this.tracer = GlobalOpenTelemetry.getTracer("telemetry-service");
        this.meter = GlobalOpenTelemetry.getMeter("telemetry-service");
    }

    @PostConstruct
    public void initializeMetrics() {
        // Initialize login metrics
        loginAttemptsCounter = meter.counterBuilder("user.login.attempts")
                .setDescription("Total login attempts")
                .setUnit("1")
                .build();

        loginDurationHistogram = meter.histogramBuilder("user.login.duration")
                .setDescription("Login operation duration")
                .setUnit("ms")
                .build();

        // Initialize page view metrics
        pageViewCounter = meter.counterBuilder("page.view.total")
                .setDescription("Total page views")
                .setUnit("1")
                .build();

        pageLoadTimeHistogram = meter.histogramBuilder("page.load.time")
                .setDescription("Page load time")
                .setUnit("ms")
                .build();

        // Initialize API call metrics
        apiCallCounter = meter.counterBuilder("api.calls.total")
                .setDescription("Total API calls")
                .setUnit("1")
                .build();

        apiDurationHistogram = meter.histogramBuilder("api.duration")
                .setDescription("API call duration")
                .setUnit("ms")
                .build();

        // Initialize error metrics
        errorCounter = meter.counterBuilder("errors.total")
                .setDescription("Total errors")
                .setUnit("1")
                .build();

        // Initialize business event metrics
        businessEventCounter = meter.counterBuilder("business.events.total")
                .setDescription("Total business events")
                .setUnit("1")
                .build();

        logger.info("Telemetry metrics initialized successfully");
    }

    /**
     * Record login event
     */
    public void recordLoginEvent(LoginEvent loginEvent) {
        logger.debug("Recording login event for user: {}", loginEvent.getUserId());

        Span span = tracer.spanBuilder("record.login.event")
                .setAttribute("user.id", loginEvent.getUserId())
                .setAttribute("login.method", loginEvent.getMethod())
                .setAttribute("login.success", loginEvent.isSuccess())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Record metrics
            Attributes attributes = Attributes.of(
                    "status", loginEvent.isSuccess() ? "success" : "failed",
                    "method", loginEvent.getMethod(),
                    "user.id", loginEvent.getUserId()
            );

            loginAttemptsCounter.add(1, attributes);

            if (loginEvent.getDurationMs() != null) {
                loginDurationHistogram.record(loginEvent.getDurationMs(), attributes);
            }

            // Record trace attributes
            if (loginEvent.getFailureReason() != null) {
                span.setAttribute("login.failure_reason", loginEvent.getFailureReason());
            }
            if (loginEvent.getIpAddress() != null) {
                span.setAttribute("user.ip_address", loginEvent.getIpAddress());
            }
            if (loginEvent.getUserAgent() != null) {
                span.setAttribute("user.user_agent", loginEvent.getUserAgent());
            }

            span.setStatus(StatusCode.OK);

            // Asynchronously process additional data
            CompletableFuture.runAsync(() -> processLoginEventData(loginEvent));

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            logger.error("Error recording login event", e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Record page view event
     */
    public void recordPageViewEvent(PageViewEvent pageViewEvent) {
        logger.debug("Recording page view event for user: {}", pageViewEvent.getUserId());

        Span span = tracer.spanBuilder("record.pageview.event")
                .setAttribute("user.id", pageViewEvent.getUserId())
                .setAttribute("page.name", pageViewEvent.getPageName())
                .setAttribute("page.url", pageViewEvent.getPageUrl())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Record metrics
            Attributes attributes = Attributes.of(
                    "page.name", pageViewEvent.getPageName(),
                    "user.id", pageViewEvent.getUserId()
            );

            pageViewCounter.add(1, attributes);

            if (pageViewEvent.getLoadTimeMs() != null) {
                pageLoadTimeHistogram.record(pageViewEvent.getLoadTimeMs(), attributes);
            }

            // Record trace attributes
            if (pageViewEvent.getReferrer() != null) {
                span.setAttribute("page.referrer", pageViewEvent.getReferrer());
            }
            if (pageViewEvent.getViewportWidth() != null) {
                span.setAttribute("page.viewport.width", pageViewEvent.getViewportWidth());
            }
            if (pageViewEvent.getViewportHeight() != null) {
                span.setAttribute("page.viewport.height", pageViewEvent.getViewportHeight());
            }

            span.setStatus(StatusCode.OK);

            // Asynchronously process additional data
            CompletableFuture.runAsync(() -> processPageViewEventData(pageViewEvent));

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            logger.error("Error recording page view event", e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Record API call event
     */
    public void recordApiCallEvent(ApiCallEvent apiCallEvent) {
        logger.debug("Recording API call event for endpoint: {}", apiCallEvent.getEndpoint());

        Span span = tracer.spanBuilder("record.apicall.event")
                .setAttribute("api.endpoint", apiCallEvent.getEndpoint())
                .setAttribute("api.method", apiCallEvent.getMethod())
                .setAttribute("api.status_code", apiCallEvent.getStatusCode())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Determine status category
            String statusCategory = getStatusCategory(apiCallEvent.getStatusCode());

            // Record metrics
            Attributes attributes = Attributes.of(
                    "endpoint", apiCallEvent.getEndpoint(),
                    "method", apiCallEvent.getMethod(),
                    "status_code", apiCallEvent.getStatusCode(),
                    "status_category", statusCategory
            );

            apiCallCounter.add(1, attributes);
            apiDurationHistogram.record(apiCallEvent.getDurationMs(), attributes);

            // Record trace attributes
            if (apiCallEvent.getUserId() != null) {
                span.setAttribute("user.id", apiCallEvent.getUserId());
            }
            if (apiCallEvent.getTraceId() != null) {
                span.setAttribute("trace.id", apiCallEvent.getTraceId());
            }
            if (apiCallEvent.getErrorMessage() != null) {
                span.setAttribute("error.message", apiCallEvent.getErrorMessage());
            }

            span.setStatus(StatusCode.OK);

            // Asynchronously process additional data
            CompletableFuture.runAsync(() -> processApiCallEventData(apiCallEvent));

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            logger.error("Error recording API call event", e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Record error event
     */
    public void recordErrorEvent(ErrorEvent errorEvent) {
        logger.debug("Recording error event: {} - {}", errorEvent.getErrorType(), errorEvent.getMessage());

        Span span = tracer.spanBuilder("record.error.event")
                .setAttribute("error.type", errorEvent.getErrorType())
                .setAttribute("error.message", errorEvent.getMessage())
                .setAttribute("error.severity", errorEvent.getSeverity().getValue())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Record metrics
            Attributes attributes = Attributes.of(
                    "error.type", errorEvent.getErrorType(),
                    "error.severity", errorEvent.getSeverity().getValue(),
                    "service.name", errorEvent.getServiceName() != null ? errorEvent.getServiceName() : "unknown"
            );

            errorCounter.add(1, attributes);

            // Record trace attributes
            if (errorEvent.getUserId() != null) {
                span.setAttribute("user.id", errorEvent.getUserId());
            }
            if (errorEvent.getStacktrace() != null) {
                span.setAttribute("error.stack_trace", errorEvent.getStacktrace());
            }
            if (errorEvent.getEndpoint() != null) {
                span.setAttribute("api.endpoint", errorEvent.getEndpoint());
            }

            span.setStatus(StatusCode.OK);

            // Asynchronously process additional data
            CompletableFuture.runAsync(() -> processErrorEventData(errorEvent));

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            logger.error("Error recording error event", e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Record business event
     */
    public void recordBusinessEvent(BusinessEvent businessEvent) {
        logger.debug("Recording business event: {} for user: {}", businessEvent.getEventType(), businessEvent.getUserId());

        Span span = tracer.spanBuilder("record.business.event")
                .setAttribute("business.event_type", businessEvent.getEventType())
                .setAttribute("user.id", businessEvent.getUserId())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Build attributes for metrics
            Attributes.Builder attributesBuilder = Attributes.builder()
                    .put("event.type", businessEvent.getEventType())
                    .put("user.id", businessEvent.getUserId());

            if (businessEvent.getCategory() != null) {
                attributesBuilder.put("category", businessEvent.getCategory());
            }
            if (businessEvent.getAction() != null) {
                attributesBuilder.put("action", businessEvent.getAction());
            }
            if (businessEvent.getObjectType() != null) {
                attributesBuilder.put("object.type", businessEvent.getObjectType());
            }

            businessEventCounter.add(1, attributesBuilder.build());

            // Record trace attributes
            if (businessEvent.getValue() != null) {
                span.setAttribute("business.value", businessEvent.getValue());
            }
            if (businessEvent.getCurrency() != null) {
                span.setAttribute("business.currency", businessEvent.getCurrency());
            }
            if (businessEvent.getFunnelStep() != null) {
                span.setAttribute("business.funnel_step", businessEvent.getFunnelStep());
            }

            span.setStatus(StatusCode.OK);

            // Asynchronously process additional data
            CompletableFuture.runAsync(() -> processBusinessEventData(businessEvent));

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            logger.error("Error recording business event", e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Process login event data asynchronously
     */
    private void processLoginEventData(LoginEvent loginEvent) {
        // Additional processing logic can be added here
        // For example: storing to database, triggering alerts, etc.
        logger.debug("Processing login event data for user: {}", loginEvent.getUserId());
    }

    /**
     * Process page view event data asynchronously
     */
    private void processPageViewEventData(PageViewEvent pageViewEvent) {
        // Additional processing logic can be added here
        logger.debug("Processing page view event data for user: {}", pageViewEvent.getUserId());
    }

    /**
     * Process API call event data asynchronously
     */
    private void processApiCallEventData(ApiCallEvent apiCallEvent) {
        // Additional processing logic can be added here
        logger.debug("Processing API call event data for endpoint: {}", apiCallEvent.getEndpoint());
    }

    /**
     * Process error event data asynchronously
     */
    private void processErrorEventData(ErrorEvent errorEvent) {
        // Additional processing logic can be added here
        logger.debug("Processing error event data: {}", errorEvent.getErrorType());
    }

    /**
     * Process business event data asynchronously
     */
    private void processBusinessEventData(BusinessEvent businessEvent) {
        // Additional processing logic can be added here
        logger.debug("Processing business event data: {}", businessEvent.getEventType());
    }

    /**
     * Get status category from HTTP status code
     */
    private String getStatusCategory(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "success";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "redirect";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "client_error";
        } else if (statusCode >= 500) {
            return "server_error";
        } else {
            return "unknown";
        }
    }
}
