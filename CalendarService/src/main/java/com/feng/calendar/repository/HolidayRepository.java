package com.feng.calendar.repository;

import com.feng.calendar.model.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Holiday entity operations
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    
    /**
     * Find holiday by country code and date
     */
    @Query("SELECT h FROM Holiday h JOIN h.country c WHERE c.code = :countryCode AND h.date = :date")
    Optional<Holiday> findByCountryCodeAndDate(@Param("countryCode") String countryCode, 
                                               @Param("date") LocalDate date);
    
    /**
     * Find holidays for a country within a date range
     */
    @Query("SELECT h FROM Holiday h JOIN h.country c WHERE c.code = :countryCode " +
           "AND h.date >= :startDate AND h.date <= :endDate ORDER BY h.date")
    List<Holiday> findByCountryCodeAndDateRange(@Param("countryCode") String countryCode,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
    
    /**
     * Find holidays by country ID and date
     */
    Optional<Holiday> findByCountryIdAndDate(Long countryId, LocalDate date);
    
    /**
     * Find all holidays for a country
     */
    @Query("SELECT h FROM Holiday h JOIN h.country c WHERE c.code = :countryCode ORDER BY h.date")
    List<Holiday> findByCountryCode(@Param("countryCode") String countryCode);
    
    /**
     * Find recurring holidays for a country
     */
    @Query("SELECT h FROM Holiday h JOIN h.country c WHERE c.code = :countryCode " +
           "AND h.isRecurring = true ORDER BY h.date")
    List<Holiday> findRecurringHolidaysByCountryCode(@Param("countryCode") String countryCode);
    
    /**
     * Find holidays by date across all countries
     */
    @Query("SELECT h FROM Holiday h JOIN h.country c WHERE h.date = :date")
    List<Holiday> findByDate(@Param("date") LocalDate date);
}
