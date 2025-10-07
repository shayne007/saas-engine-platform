package com.feng.calendar.service;

import java.time.LocalDate;
import java.util.Optional;

import com.feng.calendar.model.dto.DateTypeResponse;
import com.feng.calendar.model.entity.BusinessCalendarRule;
import com.feng.calendar.model.entity.Holiday;
import com.feng.calendar.model.enums.BusinessRuleType;
import com.feng.calendar.model.enums.DateType;
import com.feng.calendar.model.enums.HolidayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DateTypeChecker service
 */
@ExtendWith(MockitoExtension.class)
class DateTypeCheckerTest {

	@Mock
	private HolidayResolver holidayResolver;

	@Mock
	private WeekendChecker weekendChecker;

	@Mock
	private BusinessCalendarService businessCalendarService;

	private DateTypeChecker dateTypeChecker;

	@BeforeEach
	void setUp() {
		dateTypeChecker = new DateTypeChecker(holidayResolver, weekendChecker,
				businessCalendarService);
	}

	@Test
	void testCheckDateType_WorkDay() {
		// Given
		LocalDate date = LocalDate.of(2024, 7, 5); // Friday
		String countryCode = "US";

		when(weekendChecker.isWeekend(date, countryCode)).thenReturn(false);
		when(holidayResolver.findHoliday(date, countryCode)).thenReturn(Optional.empty());

		// When
		DateTypeResponse response =
				dateTypeChecker.checkDateType(date, countryCode, null);

		// Then
		assertNotNull(response);
		assertEquals(date, response.getDate());
		assertEquals(DateType.WORK_DAY, response.getDateType());
		assertTrue(response.getIsWorkDay());
		assertEquals(countryCode, response.getCountry());
	}

	@Test
	void testCheckDateType_Weekend() {
		// Given
		LocalDate date = LocalDate.of(2024, 7, 6); // Saturday
		String countryCode = "US";

		when(weekendChecker.isWeekend(date, countryCode)).thenReturn(true);

		// When
		DateTypeResponse response =
				dateTypeChecker.checkDateType(date, countryCode, null);

		// Then
		assertNotNull(response);
		assertEquals(date, response.getDate());
		assertEquals(DateType.WEEKEND, response.getDateType());
		assertFalse(response.getIsWorkDay());
		assertEquals(countryCode, response.getCountry());
	}

	@Test
	void testCheckDateType_Holiday() {
		// Given
		LocalDate date = LocalDate.of(2024, 7, 4); // Independence Day
		String countryCode = "US";

		Holiday holiday = new Holiday();
		holiday.setName("Independence Day");
		holiday.setDate(date);
		holiday.setHolidayType(HolidayType.NATIONAL);

		when(weekendChecker.isWeekend(date, countryCode)).thenReturn(false);
		when(holidayResolver.findHoliday(date, countryCode)).thenReturn(
				Optional.of(holiday));

		// When
		DateTypeResponse response =
				dateTypeChecker.checkDateType(date, countryCode, null);

		// Then
		assertNotNull(response);
		assertEquals(date, response.getDate());
		assertEquals(DateType.HOLIDAY, response.getDateType());
		assertFalse(response.getIsWorkDay());
		assertEquals("Independence Day", response.getHolidayName());
		assertEquals(countryCode, response.getCountry());
	}

	@Test
	void testCheckDateType_CustomNonWorkDay() {
		// Given
		LocalDate date = LocalDate.of(2024, 7, 5);
		String countryCode = "US";
		String businessCalendarId = "1";

		BusinessCalendarRule rule = new BusinessCalendarRule();
		rule.setRuleType(BusinessRuleType.NON_WORK_DAY);
		rule.setDescription("Company Retreat");

		when(weekendChecker.isWeekend(date, countryCode)).thenReturn(false);
		when(holidayResolver.findHoliday(date, countryCode)).thenReturn(Optional.empty());
		when(businessCalendarService.isBusinessCalendarActive(
				businessCalendarId)).thenReturn(true);
		when(businessCalendarService.getCustomRule(date, businessCalendarId)).thenReturn(
				Optional.of(rule));

		// When
		DateTypeResponse response =
				dateTypeChecker.checkDateType(date, countryCode, businessCalendarId);

		// Then
		assertNotNull(response);
		assertEquals(date, response.getDate());
		assertEquals(DateType.CUSTOM_NON_WORK_DAY, response.getDateType());
		assertFalse(response.getIsWorkDay());
		assertEquals("Company Retreat", response.getHolidayName());
		assertEquals(countryCode, response.getCountry());
	}
}
