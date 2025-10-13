package com.feng.observability.controller;

import com.feng.observability.service.TelemetryService;
import com.feng.observability.model.*;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Telemetry Data Collection Controller
 * 
 * Provides REST endpoints for collecting telemetry data from various services.
 */
@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final Tracer tracer;

    @Autowired
    public TelemetryController(TelemetryService telemetryService, Tracer tracer) {
        this.telemetryService = telemetryService;
        this.tracer = tracer;
    }

    /**
     * Collect user login events
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> collectLoginEvent(
            @Valid @RequestBody LoginEvent loginEvent) {
        
        Span span = tracer.spanBuilder("collect.login.event")
                .setAttribute("user.id", loginEvent.getUserId())
                .setAttribute("login.method", loginEvent.getMethod())
                .setAttribute("login.success", loginEvent.isSuccess())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            telemetryService.recordLoginEvent(loginEvent);
            
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Login event recorded"
            ));
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Collect page view events
     */
    @PostMapping("/pageview")
    public ResponseEntity<Map<String, Object>> collectPageViewEvent(
            @Valid @RequestBody PageViewEvent pageViewEvent) {
        
        Span span = tracer.spanBuilder("collect.pageview.event")
                .setAttribute("user.id", pageViewEvent.getUserId())
                .setAttribute("page.name", pageViewEvent.getPageName())
                .setAttribute("page.url", pageViewEvent.getPageUrl())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            telemetryService.recordPageViewEvent(pageViewEvent);
            
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Page view event recorded"
            ));
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Collect API call events
     */
    @PostMapping("/apicall")
    public ResponseEntity<Map<String, Object>> collectApiCallEvent(
            @Valid @RequestBody ApiCallEvent apiCallEvent) {
        
        Span span = tracer.spanBuilder("collect.apicall.event")
                .setAttribute("api.endpoint", apiCallEvent.getEndpoint())
                .setAttribute("api.method", apiCallEvent.getMethod())
                .setAttribute("api.status_code", apiCallEvent.getStatusCode())
                .setAttribute("api.duration_ms", apiCallEvent.getDurationMs())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            telemetryService.recordApiCallEvent(apiCallEvent);
            
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "API call event recorded"
            ));
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Collect error events
     */
    @PostMapping("/error")
    public ResponseEntity<Map<String, Object>> collectErrorEvent(
            @Valid @RequestBody ErrorEvent errorEvent) {
        
        Span span = tracer.spanBuilder("collect.error.event")
                .setAttribute("error.type", errorEvent.getErrorType())
                .setAttribute("error.message", errorEvent.getMessage())
                .setAttribute("error.severity", errorEvent.getSeverity())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            telemetryService.recordErrorEvent(errorEvent);
            
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Error event recorded"
            ));
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Collect custom business events
     */
    @PostMapping("/business")
    public ResponseEntity<Map<String, Object>> collectBusinessEvent(
            @Valid @RequestBody BusinessEvent businessEvent) {
        
        Span span = tracer.spanBuilder("collect.business.event")
                .setAttribute("business.event_type", businessEvent.getEventType())
                .setAttribute("business.user_id", businessEvent.getUserId())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            telemetryService.recordBusinessEvent(businessEvent);
            
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Business event recorded"
            ));
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "data-collector-service",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
