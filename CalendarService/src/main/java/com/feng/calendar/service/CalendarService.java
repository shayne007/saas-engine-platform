package com.feng.calendar.service;

import com.feng.calendar.exception.CountryNotFoundException;
import com.feng.calendar.model.dto.*;
import com.feng.calendar.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main service for calendar operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {
    
    private final DateTypeChecker dateTypeChecker;
    private final WorkDateFinder workDateFinder;
    private final CountryRepository countryRepository;
    
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * Check the type of a date
     */
    @Transactional(readOnly = true)
    public DateTypeResponse checkDateType(LocalDate date, String country, String timezone, String businessCalendar) {
        validateCountry(country);
        
        return dateTypeChecker.checkDateType(date, country, businessCalendar);
    }
    
    /**
     * Find the next work date
     */
    @Transactional(readOnly = true)
    public WorkDateResponse findNextWorkDate(LocalDate fromDate, String country, String businessCalendar, boolean skipWeekends) {
        validateCountry(country);
        
        if (skipWeekends) {
            return workDateFinder.findNextWorkDate(fromDate, country, businessCalendar);
        } else {
            return workDateFinder.findNextWorkDateSkipWeekendsOnly(fromDate, country);
        }
    }
    
    /**
     * Find the previous work date
     */
    @Transactional(readOnly = true)
    public WorkDateResponse findPreviousWorkDate(LocalDate fromDate, String country, String businessCalendar) {
        validateCountry(country);
        
        return workDateFinder.findPreviousWorkDate(fromDate, country, businessCalendar);
    }
    
    /**
     * Process bulk date requests
     */
    @Transactional(readOnly = true)
    public BulkDateResponse processBulkDates(BulkDateRequest request) {
        long startTime = System.currentTimeMillis();
        
        validateCountry(request.getCountry());
        
        List<DateTypeResponse> dateTypeResults = new ArrayList<>();
        List<WorkDateResponse> workDateResults = new ArrayList<>();
        
        // Parse dates
        List<LocalDate> dates = parseDates(request.getDates());
        
        // Process each date
        for (LocalDate date : dates) {
            // Check date type
            DateTypeResponse dateTypeResponse = dateTypeChecker.checkDateType(
                date, request.getCountry(), request.getBusinessCalendar());
            dateTypeResults.add(dateTypeResponse);
            
            // If requested, also find next work date
            if (request.getOperations() != null && 
                request.getOperations().contains("NEXT_WORK_DATE")) {
                WorkDateResponse workDateResponse = workDateFinder.findNextWorkDate(
                    date, request.getCountry(), request.getBusinessCalendar());
                workDateResults.add(workDateResponse);
            }
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        return BulkDateResponse.builder()
            .dateTypeResults(dateTypeResults)
            .workDateResults(workDateResults)
            .totalProcessed(dates.size())
            .processingTimeMs(processingTime)
            .build();
    }
    
    /**
     * Validate that a country exists
     */
    private void validateCountry(String countryCode) {
        if (!countryRepository.existsByCode(countryCode)) {
            throw new CountryNotFoundException(countryCode);
        }
    }
    
    /**
     * Parse date strings to LocalDate objects
     */
    private List<LocalDate> parseDates(List<String> dateStrings) {
        List<LocalDate> dates = new ArrayList<>();
        
        for (String dateString : dateStrings) {
            try {
                LocalDate date = LocalDate.parse(dateString, ISO_DATE_FORMATTER);
                dates.add(date);
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format: {}", dateString, e);
                // Skip invalid dates or throw exception based on requirements
            }
        }
        
        return dates;
    }
    
    /**
     * Get available countries
     */
    @Transactional(readOnly = true)
    public List<com.feng.calendar.model.entity.Country> getAvailableCountries() {
        return countryRepository.findAll();
    }
    
    /**
     * Check if a country is supported
     */
    @Transactional(readOnly = true)
    public boolean isCountrySupported(String countryCode) {
        return countryRepository.existsByCode(countryCode);
    }
}