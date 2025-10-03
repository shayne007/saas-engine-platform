package com.feng.calendar.model.enums;

/**
 * Enum representing the type of a date in the calendar system
 */
public enum DateType {
    /**
     * Regular working day (Monday-Friday, not a holiday)
     */
    WORK_DAY,
    
    /**
     * Weekend day (Saturday or Sunday, depending on country)
     */
    WEEKEND,
    
    /**
     * Public holiday (national, regional, or religious)
     */
    HOLIDAY,
    
    /**
     * Custom non-working day defined by business calendar rules
     */
    CUSTOM_NON_WORK_DAY
}
