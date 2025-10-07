package com.feng.calendar.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.feng.calendar.model.entity.BusinessCalendarRule;
import com.feng.calendar.model.enums.BusinessRuleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for BusinessCalendarRule entity operations
 */
@Repository
public interface BusinessCalendarRuleRepository
		extends JpaRepository<BusinessCalendarRule, Long> {

	/**
	 * Find rule for specific date and business calendar
	 */
	@Query("SELECT bcr FROM BusinessCalendarRule bcr " +
			"WHERE bcr.businessCalendar.id = :calendarId AND bcr.date = :date AND bcr" +
			".isActive = true")
	Optional<BusinessCalendarRule> findByCalendarIdAndDate(
			@Param("calendarId") Long calendarId,
			@Param("date") LocalDate date);

	/**
	 * Find all rules for a business calendar within date range
	 */
	@Query("SELECT bcr FROM BusinessCalendarRule bcr " +
			"WHERE bcr.businessCalendar.id = :calendarId " +
			"AND bcr.date >= :startDate AND bcr.date <= :endDate AND bcr.isActive = true" +
			" " +
			"ORDER BY bcr.date")
	List<BusinessCalendarRule> findByCalendarIdAndDateRange(
			@Param("calendarId") Long calendarId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	/**
	 * Find rules by type for a business calendar
	 */
	@Query("SELECT bcr FROM BusinessCalendarRule bcr " +
			"WHERE bcr.businessCalendar.id = :calendarId AND bcr.ruleType = :ruleType " +
			"AND bcr.isActive = true")
	List<BusinessCalendarRule> findByCalendarIdAndRuleType(
			@Param("calendarId") Long calendarId,
			@Param("ruleType") BusinessRuleType ruleType);

	/**
	 * Find all active rules for a business calendar
	 */
	@Query("SELECT bcr FROM BusinessCalendarRule bcr " +
			"WHERE bcr.businessCalendar.id = :calendarId AND bcr.isActive = true " +
			"ORDER BY bcr.date")
	List<BusinessCalendarRule> findActiveByCalendarId(
			@Param("calendarId") Long calendarId);

	/**
	 * Find recurring rules for a business calendar
	 */
	@Query("SELECT bcr FROM BusinessCalendarRule bcr " +
			"WHERE bcr.businessCalendar.id = :calendarId " +
			"AND bcr.recurrenceRule IS NOT NULL AND bcr.isActive = true")
	List<BusinessCalendarRule> findRecurringByCalendarId(
			@Param("calendarId") Long calendarId);
}
