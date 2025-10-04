package com.feng.calendar.service;

import com.feng.calendar.model.entity.Country;
import com.feng.calendar.model.entity.WeekendDefinition;
import com.feng.calendar.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for checking if a date is a weekend
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeekendChecker {
    
    private final CountryRepository countryRepository;
    
    // In-memory cache for weekend definitions
    private final Map<String, List<WeekendDefinition>> weekendCache = new ConcurrentHashMap<>();
    
    /**
     * Check if a date is a weekend for a given country
     */
    @Cacheable(value = "weekend-checks", key = "#p0.toString() + '_' + #p1")
    public boolean isWeekend(LocalDate date, String countryCode) {
        try {
            List<WeekendDefinition> weekendDefinitions = getWeekendDefinitions(countryCode);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            int dayOfWeekValue = dayOfWeek.getValue(); // 1=Monday, 7=Sunday
            
            return weekendDefinitions.stream()
                .anyMatch(wd -> wd.getDayOfWeek().equals(dayOfWeekValue) && wd.getIsWeekend());
                
        } catch (Exception e) {
            log.warn("Error checking weekend for date {} in country {}", date, countryCode, e);
            // Fallback to default weekend (Saturday=6, Sunday=7)
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }
    }
    
    /**
     * Get weekend definitions for a country with caching
     */
    private List<WeekendDefinition> getWeekendDefinitions(String countryCode) {
        return weekendCache.computeIfAbsent(countryCode, this::loadWeekendDefinitions);
    }
    
    /**
     * Load weekend definitions from database
     */
    private List<WeekendDefinition> loadWeekendDefinitions(String countryCode) {
        return countryRepository.findByCodeWithWeekendDefinitions(countryCode)
            .map(Country::getWeekendDefinitions)
            .orElseGet(this::getDefaultWeekendDefinitions);
    }
    
    /**
     * Get default weekend definitions (Saturday and Sunday)
     */
    private List<WeekendDefinition> getDefaultWeekendDefinitions() {
        WeekendDefinition saturday = new WeekendDefinition();
        saturday.setDayOfWeek(6);
        saturday.setIsWeekend(true);
        
        WeekendDefinition sunday = new WeekendDefinition();
        sunday.setDayOfWeek(7);
        sunday.setIsWeekend(true);
        
        return List.of(saturday, sunday);
    }
    
    /**
     * Clear weekend cache for a country
     */
    public void clearWeekendCache(String countryCode) {
        weekendCache.remove(countryCode);
    }
    
    /**
     * Clear all weekend cache
     */
    public void clearAllWeekendCache() {
        weekendCache.clear();
    }
}