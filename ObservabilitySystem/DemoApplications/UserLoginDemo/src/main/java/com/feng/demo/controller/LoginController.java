package com.feng.demo.controller;

import com.feng.demo.service.LoginService;
import com.feng.demo.model.LoginRequest;
import com.feng.demo.model.LoginResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
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
 * Login Controller
 * 
 * Demonstrates user login tracking with comprehensive telemetry.
 */
@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;
    private final Tracer tracer;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
        this.tracer = GlobalOpenTelemetry.getTracer("user-login-demo");
    }

    /**
     * Authenticate user login
     */
    @PostMapping
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Span span = tracer.spanBuilder("POST /api/login")
                .setAttribute("user.username", request.getUsername())
                .setAttribute("login.method", request.getMethod())
                .setAttribute("http.method", "POST")
                .setAttribute("http.route", "/api/login")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            LoginResponse response = loginService.authenticateUser(request);
            
            span.setAttribute("login.success", response.isSuccess());
            span.setAttribute("login.duration_ms", response.getDurationMs());
            
            if (response.isSuccess()) {
                span.setAttribute("user.id", response.getUserId());
                span.setAttribute("session.id", response.getSessionId());
                span.setStatus(StatusCode.OK);
                return ResponseEntity.ok(response);
            } else {
                span.setAttribute("login.failure_reason", response.getErrorMessage());
                span.setStatus(StatusCode.ERROR, response.getErrorMessage());
                return ResponseEntity.status(401).body(response);
            }
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            
            LoginResponse errorResponse = new LoginResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Internal server error: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        } finally {
            span.end();
        }
    }

    /**
     * Logout user
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestParam String sessionId) {
        Span span = tracer.spanBuilder("POST /api/login/logout")
                .setAttribute("session.id", sessionId)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            boolean success = loginService.logoutUser(sessionId);
            
            span.setAttribute("logout.success", success);
            span.setStatus(StatusCode.OK);
            
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "Logout successful" : "Session not found"
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
     * Validate session
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSession(@RequestParam String sessionId) {
        Span span = tracer.spanBuilder("GET /api/login/validate")
                .setAttribute("session.id", sessionId)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            boolean valid = loginService.validateSession(sessionId);
            
            span.setAttribute("session.valid", valid);
            span.setStatus(StatusCode.OK);
            
            return ResponseEntity.ok(Map.of(
                    "valid", valid,
                    "sessionId", sessionId
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
     * Get user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@RequestParam String userId) {
        Span span = tracer.spanBuilder("GET /api/login/profile")
                .setAttribute("user.id", userId)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Map<String, Object> profile = loginService.getUserProfile(userId);
            
            span.setAttribute("profile.found", !profile.isEmpty());
            span.setStatus(StatusCode.OK);
            
            return ResponseEntity.ok(profile);
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
