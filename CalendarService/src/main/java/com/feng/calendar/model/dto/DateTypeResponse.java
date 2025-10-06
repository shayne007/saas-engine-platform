package com.feng.calendar.model.dto;

import com.feng.calendar.model.enums.DateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for date type checking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateTypeResponse {
    
    private LocalDate date;
    private DateType dateType;
    private Boolean isWorkDay;
    private String holidayName;
    private String country;
    private DateMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateMetadata {
        private String dayOfWeek;
        private Integer weekNumber;
        private Boolean isWeekend;
    }
}
