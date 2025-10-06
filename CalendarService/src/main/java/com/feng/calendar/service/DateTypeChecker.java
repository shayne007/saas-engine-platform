package com.feng.calendar.service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import com.feng.calendar.model.dto.DateTypeResponse;
import com.feng.calendar.model.entity.BusinessCalendarRule;
import com.feng.calendar.model.entity.Holiday;
import com.feng.calendar.model.enums.DateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Service for checking the type of a date (work day, holiday, weekend, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DateTypeChecker {

	private final HolidayResolver holidayResolver;

	private final WeekendChecker weekendChecker;

	private final BusinessCalendarService businessCalendarService;

	/**
	 * Check the type of a date
	 */
	public DateTypeResponse checkDateType(LocalDate date, String countryCode,
			String businessCalendarId) {
		// Check if it's a weekend first (fastest check)
		if (weekendChecker.isWeekend(date, countryCode)) {
			return createWeekendResponse(date, countryCode);
		}

		// Check for holidays
		Optional<Holiday> holiday = holidayResolver.findHoliday(date, countryCode);
		if (holiday.isPresent()) {
			return createHolidayResponse(date, holiday.get(), countryCode);
		}

		// Check custom business calendar rules
		if (businessCalendarId != null &&
				businessCalendarService.isBusinessCalendarActive(businessCalendarId)) {
			Optional<BusinessCalendarRule> customRule =
					businessCalendarService.getCustomRule(date, businessCalendarId);
			if (customRule.isPresent() && !customRule.get().isWorkDay()) {
				return createCustomNonWorkDayResponse(date, customRule.get(),
						countryCode);
			}
		}

		// Default to work day
		return createWorkDayResponse(date, countryCode);
	}

	/**
	 * Create response for work day
	 */
	private DateTypeResponse createWorkDayResponse(LocalDate date, String countryCode) {
		return DateTypeResponse.builder()
				.date(date)
				.dateType(DateType.WORK_DAY)
				.isWorkDay(true)
				.country(countryCode)
				.metadata(createMetadata(date))
				.build();
	}

	/**
	 * Create response for weekend
	 */
	private DateTypeResponse createWeekendResponse(LocalDate date, String countryCode) {
		return DateTypeResponse.builder()
				.date(date)
				.dateType(DateType.WEEKEND)
				.isWorkDay(false)
				.country(countryCode)
				.metadata(createMetadata(date))
				.build();
	}

	/**
	 * Create response for holiday
	 */
	private DateTypeResponse createHolidayResponse(LocalDate date, Holiday holiday,
			String countryCode) {
		return DateTypeResponse.builder()
				.date(date)
				.dateType(DateType.HOLIDAY)
				.isWorkDay(false)
				.holidayName(holiday.getName())
				.country(countryCode)
				.metadata(createMetadata(date))
				.build();
	}

	/**
	 * Create response for custom non-work day
	 */
	private DateTypeResponse createCustomNonWorkDayResponse(LocalDate date,
			BusinessCalendarRule rule, String countryCode) {
		return DateTypeResponse.builder()
				.date(date)
				.dateType(DateType.CUSTOM_NON_WORK_DAY)
				.isWorkDay(false)
				.holidayName(rule.getDescription())
				.country(countryCode)
				.metadata(createMetadata(date))
				.build();
	}

	/**
	 * Create metadata for a date
	 */
	private DateTypeResponse.DateMetadata createMetadata(LocalDate date) {
		return DateTypeResponse.DateMetadata.builder()
				.dayOfWeek(date.getDayOfWeek()
						.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
				.weekNumber(date.getDayOfYear() / 7 + 1)
				.isWeekend(weekendChecker.isWeekend(date,
						"US")) // Default to US weekend check for metadata
				.build();
	}
}