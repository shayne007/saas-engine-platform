package com.feng.calendar.exception;

/**
 * Exception thrown when a business calendar is not found
 */
public class BusinessCalendarNotFoundException extends CalendarServiceException {

	public BusinessCalendarNotFoundException(String businessCalendarId) {
		super("Business calendar not found: " + businessCalendarId);
	}

	public BusinessCalendarNotFoundException(Long businessCalendarId) {
		super("Business calendar not found: " + businessCalendarId);
	}
}
