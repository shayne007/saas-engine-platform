package com.feng.calendar.web;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feng.calendar.model.dto.BulkDateRequest;
import com.feng.calendar.model.dto.DateTypeResponse;
import com.feng.calendar.model.dto.WorkDateResponse;
import com.feng.calendar.model.enums.DateType;
import com.feng.calendar.service.CalendarService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CalendarController
 */
@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CalendarService calendarService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void testGetDateType() throws Exception {
		// Given
		LocalDate date = LocalDate.of(2024, 7, 4);
		DateTypeResponse expectedResponse = DateTypeResponse.builder()
				.date(date)
				.dateType(DateType.HOLIDAY)
				.isWorkDay(false)
				.holidayName("Independence Day")
				.country("US")
				.build();

		when(calendarService.checkDateType(eq(date), eq("US"), anyString()))
				.thenReturn(expectedResponse);

		// When & Then
		mockMvc.perform(get("/api/v1/calendar/date-type")
						.param("date", "2024-07-04")
						.param("country", "US"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.date").value("2024-07-04"))
				.andExpect(jsonPath("$.dateType").value("HOLIDAY"))
				.andExpect(jsonPath("$.isWorkDay").value(false))
				.andExpect(jsonPath("$.holidayName").value("Independence Day"))
				.andExpect(jsonPath("$.country").value("US"));
	}

	@Test
	void testGetNextWorkDate() throws Exception {
		// Given
		LocalDate fromDate = LocalDate.of(2024, 7, 4);
		LocalDate nextWorkDate = LocalDate.of(2024, 7, 5);

		WorkDateResponse expectedResponse = WorkDateResponse.builder()
				.fromDate(fromDate)
				.nextWorkDate(nextWorkDate)
				.daysSkipped(1)
				.skippedDates(List.of(
						WorkDateResponse.SkippedDate.builder()
								.date(fromDate.plusDays(1))
								.reason("Independence Day Holiday")
								.build()
				))
				.build();

		when(calendarService.findNextWorkDate(eq(fromDate), eq("US"), anyString(),
				anyBoolean()))
				.thenReturn(expectedResponse);

		// When & Then
		mockMvc.perform(get("/api/v1/calendar/next-work-date")
						.param("fromDate", "2024-07-04")
						.param("country", "US"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.fromDate").value("2024-07-04"))
				.andExpect(jsonPath("$.nextWorkDate").value("2024-07-05"))
				.andExpect(jsonPath("$.daysSkipped").value(1))
				.andExpect(jsonPath("$.skippedDates").isArray())
				.andExpect(jsonPath("$.skippedDates[0].reason").value(
						"Independence Day Holiday"));
	}

	@Test
	void testBulkCheck() throws Exception {
		// Given
		BulkDateRequest request = BulkDateRequest.builder()
				.dates(List.of("2024-07-04", "2024-07-05"))
				.country("US")
				.operations(List.of("DATE_TYPE"))
				.build();

		DateTypeResponse response1 = DateTypeResponse.builder()
				.date(LocalDate.of(2024, 7, 4))
				.dateType(DateType.HOLIDAY)
				.isWorkDay(false)
				.holidayName("Independence Day")
				.country("US")
				.build();

		DateTypeResponse response2 = DateTypeResponse.builder()
				.date(LocalDate.of(2024, 7, 5))
				.dateType(DateType.WORK_DAY)
				.isWorkDay(true)
				.country("US")
				.build();

		when(calendarService.processBulkDates(any(BulkDateRequest.class)))
				.thenReturn(com.feng.calendar.model.dto.BulkDateResponse.builder()
						.dateTypeResults(List.of(response1, response2))
						.totalProcessed(2)
						.processingTimeMs(100L)
						.build());

		// When & Then
		mockMvc.perform(post("/api/v1/calendar/bulk-check")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalProcessed").value(2))
				.andExpect(jsonPath("$.processingTimeMs").value(100))
				.andExpect(jsonPath("$.dateTypeResults").isArray())
				.andExpect(jsonPath("$.dateTypeResults[0].dateType").value("HOLIDAY"))
				.andExpect(jsonPath("$.dateTypeResults[1].dateType").value("WORK_DAY"));
	}

	@Test
	void testGetCountries() throws Exception {
		// When & Then
		mockMvc.perform(get("/api/v1/calendar/countries"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	void testIsCountrySupported() throws Exception {
		// Given
		when(calendarService.isCountrySupported("US")).thenReturn(true);

		// When & Then
		mockMvc.perform(get("/api/v1/calendar/countries/US/supported"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").value(true));
	}
}
