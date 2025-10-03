package com.feng.calendar.exception;

/**
 * Exception thrown when a country is not found
 */
public class CountryNotFoundException extends CalendarServiceException {
    
    public CountryNotFoundException(String countryCode) {
        super("Country not found: " + countryCode);
    }
}
