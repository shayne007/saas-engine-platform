package com.feng.calendar.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for bulk date processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDateRequest {

	@NotEmpty(message = "Dates list cannot be empty")
	private List<String> dates; // ISO date strings

	@NotNull(message = "Country code is required")
	private String country;

	private String businessCalendar;

	private List<String> operations; // DATE_TYPE, NEXT_WORK_DATE, etc.
}
