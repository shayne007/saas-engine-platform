package com.feng.calendar.service;

import com.feng.calendar.exception.BusinessCalendarNotFoundException;
import com.feng.calendar.model.entity.BusinessCalendar;
import com.feng.calendar.model.entity.BusinessCalendarRule;
import com.feng.calendar.repository.BusinessCalendarRepository;
import com.feng.calendar.repository.BusinessCalendarRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service for managing business calendar rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessCalendarService {
    
    private final BusinessCalendarRepository businessCalendarRepository;
    private final BusinessCalendarRuleRepository businessCalendarRuleRepository;
    
    /**
     * Get custom rule for a specific date and business calendar
     */
    @Cacheable(value = "business-calendar-rules", key = "#businessCalendarId + '_' + #date.toString()")
    public Optional<BusinessCalendarRule> getCustomRule(LocalDate date, String businessCalendarId) {
        try {
            Long calendarId = Long.parseLong(businessCalendarId);
            return businessCalendarRuleRepository.findByCalendarIdAndDate(calendarId, date);
        } catch (NumberFormatException e) {
            log.warn("Invalid business calendar ID format: {}", businessCalendarId);
            return Optional.empty();
        }
    }
    
    /**
     * Get business calendar by ID
     */
    public BusinessCalendar getBusinessCalendar(Long calendarId) {
        return businessCalendarRepository.findByIdWithRules(calendarId)
            .orElseThrow(() -> new BusinessCalendarNotFoundException(calendarId));
    }
    
    /**
     * Get business calendar by organization ID and country code
     */
    public Optional<BusinessCalendar> getBusinessCalendarByOrganizationAndCountry(String organizationId, String countryCode) {
        return businessCalendarRepository.findByOrganizationIdAndCountryCode(organizationId, countryCode);
    }
    
    /**
     * Check if a business calendar exists and is active
     */
    public boolean isBusinessCalendarActive(String businessCalendarId) {
        try {
            Long calendarId = Long.parseLong(businessCalendarId);
            return businessCalendarRepository.findById(calendarId)
                .map(BusinessCalendar::getIsActive)
                .orElse(false);
        } catch (NumberFormatException e) {
            log.warn("Invalid business calendar ID format: {}", businessCalendarId);
            return false;
        }
    }
    
    /**
     * Get all active rules for a business calendar within date range
     */
    public java.util.List<BusinessCalendarRule> getRulesForDateRange(Long calendarId, LocalDate startDate, LocalDate endDate) {
        return businessCalendarRuleRepository.findByCalendarIdAndDateRange(calendarId, startDate, endDate);
    }
    
    /**
     * Clear business calendar cache for a specific calendar and date
     */
    public void clearBusinessCalendarCache(Long calendarId, LocalDate date) {
        // This would be implemented with cache eviction if using Spring Cache
        // For now, we'll rely on TTL
    }
    
    /**
     * Clear all business calendar cache for a calendar
     */
    public void clearBusinessCalendarCache(Long calendarId) {
        // This would be implemented with cache eviction if using Spring Cache
        // For now, we'll rely on TTL
    }
}