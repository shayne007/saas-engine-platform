package com.feng.calendar.service;

import com.feng.calendar.exception.CalendarServiceException;
import com.feng.calendar.model.dto.DateTypeResponse;
import com.feng.calendar.model.dto.WorkDateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for finding next and previous work dates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkDateFinder {
    
    private final DateTypeChecker dateTypeChecker;
    private static final int MAX_SEARCH_DAYS = 30; // Prevent infinite loops
    
    /**
     * Find the next work date from a given date
     */
    public WorkDateResponse findNextWorkDate(LocalDate fromDate, String countryCode, String businessCalendarId) {
        LocalDate currentDate = fromDate.plusDays(1);
        List<WorkDateResponse.SkippedDate> skippedDates = new ArrayList<>();
        int daysChecked = 0;
        
        while (daysChecked < MAX_SEARCH_DAYS) {
            DateTypeResponse dateCheck = dateTypeChecker.checkDateType(currentDate, countryCode, businessCalendarId);
            
            if (dateCheck.getIsWorkDay()) {
                return WorkDateResponse.builder()
                    .fromDate(fromDate)
                    .nextWorkDate(currentDate)
                    .daysSkipped(daysChecked)
                    .skippedDates(skippedDates)
                    .build();
            }
            
            // Add to skipped dates
            skippedDates.add(createSkippedDate(currentDate, dateCheck));
            
            currentDate = currentDate.plusDays(1);
            daysChecked++;
        }
        
        throw new CalendarServiceException(
            "Could not find next work date within " + MAX_SEARCH_DAYS + " days from " + fromDate);
    }
    
    /**
     * Find the previous work date from a given date
     */
    public WorkDateResponse findPreviousWorkDate(LocalDate fromDate, String countryCode, String businessCalendarId) {
        LocalDate currentDate = fromDate.minusDays(1);
        List<WorkDateResponse.SkippedDate> skippedDates = new ArrayList<>();
        int daysChecked = 0;
        
        while (daysChecked < MAX_SEARCH_DAYS) {
            DateTypeResponse dateCheck = dateTypeChecker.checkDateType(currentDate, countryCode, businessCalendarId);
            
            if (dateCheck.getIsWorkDay()) {
                return WorkDateResponse.builder()
                    .fromDate(fromDate)
                    .previousWorkDate(currentDate)
                    .daysSkipped(daysChecked)
                    .skippedDates(skippedDates)
                    .build();
            }
            
            skippedDates.add(createSkippedDate(currentDate, dateCheck));
            
            currentDate = currentDate.minusDays(1);
            daysChecked++;
        }
        
        throw new CalendarServiceException(
            "Could not find previous work date within " + MAX_SEARCH_DAYS + " days from " + fromDate);
    }
    
    /**
     * Find the next work date skipping only weekends (not holidays)
     */
    public WorkDateResponse findNextWorkDateSkipWeekendsOnly(LocalDate fromDate, String countryCode) {
        LocalDate currentDate = fromDate.plusDays(1);
        List<WorkDateResponse.SkippedDate> skippedDates = new ArrayList<>();
        int daysChecked = 0;
        
        while (daysChecked < MAX_SEARCH_DAYS) {
            DateTypeResponse dateCheck = dateTypeChecker.checkDateType(currentDate, countryCode, null);
            
            // Only skip weekends, not holidays
            if (dateCheck.getDateType() != com.feng.calendar.model.enums.DateType.WEEKEND) {
                return WorkDateResponse.builder()
                    .fromDate(fromDate)
                    .nextWorkDate(currentDate)
                    .daysSkipped(daysChecked)
                    .skippedDates(skippedDates)
                    .build();
            }
            
            skippedDates.add(createSkippedDate(currentDate, dateCheck));
            
            currentDate = currentDate.plusDays(1);
            daysChecked++;
        }
        
        throw new CalendarServiceException(
            "Could not find next work date (weekends only) within " + MAX_SEARCH_DAYS + " days from " + fromDate);
    }
    
    /**
     * Create a skipped date entry
     */
    private WorkDateResponse.SkippedDate createSkippedDate(LocalDate date, DateTypeResponse dateCheck) {
        String reason = getSkipReason(dateCheck);
        
        return WorkDateResponse.SkippedDate.builder()
            .date(date)
            .reason(reason)
            .dateType(dateCheck.getDateType().toString())
            .build();
    }
    
    /**
     * Get the reason for skipping a date
     */
    private String getSkipReason(DateTypeResponse dateCheck) {
        return switch (dateCheck.getDateType()) {
            case WEEKEND -> "Weekend";
            case HOLIDAY -> dateCheck.getHolidayName() != null ? 
                dateCheck.getHolidayName() + " Holiday" : "Holiday";
            case CUSTOM_NON_WORK_DAY -> dateCheck.getHolidayName() != null ? 
                dateCheck.getHolidayName() : "Custom Non-Work Day";
            default -> "Non-Work Day";
        };
    }
}