package com.feng.calendar.exception;

import java.time.LocalDate;

/**
 * Exception thrown when an invalid date is provided
 */
public class InvalidDateException extends CalendarServiceException {

	public InvalidDateException(String date) {
		super("Invalid date format: " + date);
	}

	public InvalidDateException(LocalDate date) {
		super("Invalid date: " + date);
	}

	public InvalidDateException(String message, Throwable cause) {
		super(message, cause);
	}
}
