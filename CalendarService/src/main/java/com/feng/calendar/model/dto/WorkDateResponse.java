package com.feng.calendar.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for work date finding operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkDateResponse {
    
    private LocalDate fromDate;
    private LocalDate nextWorkDate;
    private LocalDate previousWorkDate; // For previous work date responses
    private Integer daysSkipped;
    private List<SkippedDate> skippedDates;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedDate {
        private LocalDate date;
        private String reason;
        private String dateType;
    }
}
