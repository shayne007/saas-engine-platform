package com.feng.calendar.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for bulk date processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDateResponse {
    
    private List<DateTypeResponse> dateTypeResults;
    private List<WorkDateResponse> workDateResults;
    private Integer totalProcessed;
    private Long processingTimeMs;
}
