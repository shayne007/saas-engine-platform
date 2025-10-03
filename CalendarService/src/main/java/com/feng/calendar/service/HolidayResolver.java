package com.feng.calendar.service;

import com.feng.calendar.model.entity.Holiday;
import com.feng.calendar.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Service for resolving holidays with multi-level caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayResolver {
    
    private final HolidayRepository holidayRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ExternalHolidayClient externalHolidayClient;
    
    private static final String HOLIDAY_CACHE_KEY = "holiday:%s:%s"; // country:date
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    /**
     * Find holiday for a specific date and country
     */
    public Optional<Holiday> findHoliday(LocalDate date, String countryCode) {
        String cacheKey = String.format(HOLIDAY_CACHE_KEY, countryCode, date.toString());
        
        // Try L1 cache (Redis) first
        Holiday cachedHoliday = (Holiday) redisTemplate.opsForValue().get(cacheKey);
        if (cachedHoliday != null) {
            return cachedHoliday.isNoHoliday() ? Optional.empty() : Optional.of(cachedHoliday);
        }
        
        // Check database
        Optional<Holiday> holiday = holidayRepository.findByCountryCodeAndDate(countryCode, date);
        
        if (holiday.isPresent()) {
            cacheHoliday(cacheKey, holiday.get());
            return holiday;
        }
        
        // Check external APIs for real-time data
        try {
            Optional<Holiday> externalHoliday = externalHolidayClient.fetchHoliday(date, countryCode);
            
            if (externalHoliday.isPresent()) {
                // Save to database for future use
                Holiday savedHoliday = holidayRepository.save(externalHoliday.get());
                cacheHoliday(cacheKey, savedHoliday);
                return Optional.of(savedHoliday);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch holiday from external API for date {} in country {}", date, countryCode, e);
        }
        
        // Cache negative result
        Holiday noHoliday = Holiday.noHoliday();
        cacheHoliday(cacheKey, noHoliday);
        
        return Optional.empty();
    }
    
    /**
     * Cache holiday result
     */
    private void cacheHoliday(String cacheKey, Holiday holiday) {
        try {
            redisTemplate.opsForValue().set(cacheKey, holiday, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache holiday for key: {}", cacheKey, e);
        }
    }
    
    /**
     * Clear holiday cache for a specific date and country
     */
    public void clearHolidayCache(LocalDate date, String countryCode) {
        String cacheKey = String.format(HOLIDAY_CACHE_KEY, countryCode, date.toString());
        redisTemplate.delete(cacheKey);
    }
    
    /**
     * Clear all holiday cache for a country
     */
    public void clearHolidayCacheForCountry(String countryCode) {
        String pattern = String.format(HOLIDAY_CACHE_KEY.replace(":%s", ""), countryCode) + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}