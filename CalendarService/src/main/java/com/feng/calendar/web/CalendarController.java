package com.feng.calendar.web;

import com.feng.calendar.exception.CalendarServiceException;
import com.feng.calendar.model.dto.*;
import com.feng.calendar.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Instant;

/**
 * REST Controller for Calendar Service API endpoints
 */
@RestController
@RequestMapping("/api/v1/calendar")
@Validated
@RequiredArgsConstructor
@Slf4j
public class CalendarController {
    
    private final CalendarService calendarService;
    
    /**
     * Check the type of a date
     */
    @GetMapping("/date-type")
    public ResponseEntity<DateTypeResponse> getDateType(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "country", defaultValue = "US") String country,
            @RequestParam(name = "timezone", required = false) String timezone,
            @RequestParam(name = "businessCalendar", required = false) String businessCalendar) {
        
        log.info("Checking date type for date: {}, country: {}, businessCalendar: {}", 
                date, country, businessCalendar);
        
        DateTypeResponse response = calendarService.checkDateType(date, country, timezone, businessCalendar);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the next work date
     */
    @GetMapping("/next-work-date")
    public ResponseEntity<WorkDateResponse> getNextWorkDate(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "country", defaultValue = "US") String country,
            @RequestParam(name = "businessCalendar", required = false) String businessCalendar,
            @RequestParam(name = "skipWeekends", defaultValue = "true") boolean skipWeekends) {
        
        log.info("Finding next work date from: {}, country: {}, businessCalendar: {}, skipWeekends: {}", 
                fromDate, country, businessCalendar, skipWeekends);
        
        WorkDateResponse response = calendarService.findNextWorkDate(fromDate, country, businessCalendar, skipWeekends);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the previous work date
     */
    @GetMapping("/previous-work-date")
    public ResponseEntity<WorkDateResponse> getPreviousWorkDate(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "country", defaultValue = "US") String country,
            @RequestParam(name = "businessCalendar", required = false) String businessCalendar) {
        
        log.info("Finding previous work date from: {}, country: {}, businessCalendar: {}", 
                fromDate, country, businessCalendar);
        
        WorkDateResponse response = calendarService.findPreviousWorkDate(fromDate, country, businessCalendar);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Bulk date processing
     */
    @PostMapping("/bulk-check")
    public ResponseEntity<BulkDateResponse> bulkCheck(@Valid @RequestBody BulkDateRequest request) {
        log.info("Processing bulk date check for {} dates in country: {}", 
                request.getDates().size(), request.getCountry());
        
        BulkDateResponse response = calendarService.processBulkDates(request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get available countries
     */
    @GetMapping("/countries")
    public ResponseEntity<java.util.List<com.feng.calendar.model.entity.Country>> getCountries() {
        log.info("Retrieving available countries");
        
        java.util.List<com.feng.calendar.model.entity.Country> countries = calendarService.getAvailableCountries();
        
        return ResponseEntity.ok(countries);
    }
    
    /**
     * Check if a country is supported
     */
    @GetMapping("/countries/{countryCode}/supported")
    public ResponseEntity<Boolean> isCountrySupported(@PathVariable("countryCode") String countryCode) {
        log.info("Checking if country is supported: {}", countryCode);
        
        boolean supported = calendarService.isCountrySupported(countryCode);
        
        return ResponseEntity.ok(supported);
    }
    
    /**
     * Exception handler for CalendarServiceException
     */
    @ExceptionHandler(CalendarServiceException.class)
    public ResponseEntity<ErrorResponse> handleCalendarException(CalendarServiceException e) {
        log.error("Calendar service exception: {}", e.getMessage(), e);
        
        ErrorResponse error = ErrorResponse.builder()
            .error("CALENDAR_ERROR")
            .message(e.getMessage())
            .timestamp(Instant.now())
            .status(400)
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException e) {
        log.error("Validation exception: {}", e.getMessage(), e);
        
        ErrorResponse error = ErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message("Invalid request parameters: " + e.getMessage())
            .timestamp(Instant.now())
            .status(400)
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Generic exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        
        ErrorResponse error = ErrorResponse.builder()
            .error("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .timestamp(Instant.now())
            .status(500)
            .build();
        
        return ResponseEntity.internalServerError().body(error);
    }
}