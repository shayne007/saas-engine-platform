package com.feng.calendar.model.enums;

/**
 * Enum representing the type of business calendar rule
 */
public enum BusinessRuleType {
    /**
     * Custom holiday rule
     */
    HOLIDAY,
    
    /**
     * Custom work day rule (e.g., Saturday work day)
     */
    WORK_DAY,
    
    /**
     * Weekend override rule (e.g., make Sunday a work day)
     */
    WEEKEND_OVERRIDE,
    
    /**
     * Custom non-working day rule
     */
    NON_WORK_DAY
}
