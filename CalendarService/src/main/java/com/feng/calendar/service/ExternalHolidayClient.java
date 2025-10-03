package com.feng.calendar.service;

import com.feng.calendar.model.entity.Holiday;
import com.feng.calendar.model.entity.Country;
import com.feng.calendar.model.enums.HolidayType;
import com.feng.calendar.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Client for fetching holiday data from external APIs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalHolidayClient {
    
    private final WebClient.Builder webClientBuilder;
    private final CountryRepository countryRepository;
    
    private static final String HOLIDAY_API_URL = "https://api.holidayapi.com";
    private static final String GOVERNMENT_API_US = "https://api.usa.gov/holidays";
    private static final String GOVERNMENT_API_GB = "https://api.gov.uk/bank-holidays";
    
    /**
     * Fetch holiday from external API
     */
    public Optional<Holiday> fetchHoliday(LocalDate date, String countryCode) {
        try {
            return switch (countryCode.toUpperCase()) {
                case "US" -> fetchUSHoliday(date);
                case "GB", "UK" -> fetchGBHoliday(date);
                default -> fetchGenericHoliday(date, countryCode);
            };
        } catch (Exception e) {
            log.warn("Failed to fetch holiday from external API for date {} in country {}", date, countryCode, e);
            return Optional.empty();
        }
    }
    
    /**
     * Fetch US holiday from government API
     */
    private Optional<Holiday> fetchUSHoliday(LocalDate date) {
        try {
            WebClient client = webClientBuilder.baseUrl(GOVERNMENT_API_US).build();
            
            String response = client.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("year", date.getYear())
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response and find holiday for specific date
            // This is a simplified implementation - in reality you'd parse JSON
            if (response != null && response.contains("Independence Day") && 
                date.getMonthValue() == 7 && date.getDayOfMonth() == 4) {
                return createHoliday(date, "US", "Independence Day", HolidayType.NATIONAL);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to fetch US holiday for date {}", date, e);
            return Optional.empty();
        }
    }
    
    /**
     * Fetch GB holiday from government API
     */
    private Optional<Holiday> fetchGBHoliday(LocalDate date) {
        try {
            WebClient client = webClientBuilder.baseUrl(GOVERNMENT_API_GB).build();
            
            String response = client.get()
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response and find holiday for specific date
            // This is a simplified implementation - in reality you'd parse JSON
            if (response != null && response.contains("Christmas Day") && 
                date.getMonthValue() == 12 && date.getDayOfMonth() == 25) {
                return createHoliday(date, "GB", "Christmas Day", HolidayType.NATIONAL);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to fetch GB holiday for date {}", date, e);
            return Optional.empty();
        }
    }
    
    /**
     * Fetch generic holiday from holiday API
     */
    private Optional<Holiday> fetchGenericHoliday(LocalDate date, String countryCode) {
        try {
            WebClient client = webClientBuilder.baseUrl(HOLIDAY_API_URL).build();
            
            String response = client.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/holidays")
                    .queryParam("country", countryCode)
                    .queryParam("year", date.getYear())
                    .queryParam("month", date.getMonthValue())
                    .queryParam("day", date.getDayOfMonth())
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response and create holiday
            // This is a simplified implementation - in reality you'd parse JSON
            if (response != null && !response.isEmpty()) {
                return createHoliday(date, countryCode, "External Holiday", HolidayType.NATIONAL);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to fetch generic holiday for date {} in country {}", date, countryCode, e);
            return Optional.empty();
        }
    }
    
    /**
     * Create holiday entity from external data
     */
    private Optional<Holiday> createHoliday(LocalDate date, String countryCode, String name, HolidayType holidayType) {
        return countryRepository.findByCode(countryCode)
            .map(country -> {
                Holiday holiday = new Holiday();
                holiday.setCountry(country);
                holiday.setName(name);
                holiday.setDate(date);
                holiday.setHolidayType(holidayType);
                holiday.setIsRecurring(false);
                return holiday;
            });
    }
}