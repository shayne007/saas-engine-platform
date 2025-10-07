package com.feng.calendar.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.feng.calendar.model.enums.HolidayType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity representing a holiday
 */
@Entity
@Table(name = "holidays",
		indexes = {
				@Index(name = "idx_holidays_country_date",
						columnList = "country_id, date"),
				@Index(name = "idx_holidays_date", columnList = "date")
		})
@Data
@EqualsAndHashCode(exclude = "country")
@ToString(exclude = "country")
public class Holiday {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "country_id", nullable = false)
	private Country country;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false)
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	@Column(name = "holiday_type", nullable = false, length = 50)
	private HolidayType holidayType;

	@Column(name = "is_recurring", nullable = false)
	private Boolean isRecurring = false;

	@Column(name = "recurrence_rule", columnDefinition = "TEXT")
	private String recurrenceRule;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * Transient field to support serialization/deserialization
	 * Not persisted to database
	 */
	@Transient
	private Boolean noHoliday;

	/**
	 * Static method to create a "no holiday" marker for caching
	 */
	public static Holiday noHoliday() {
		Holiday holiday = new Holiday();
		holiday.setId(-1L);
		holiday.setName("NO_HOLIDAY");
		holiday.setNoHoliday(true);
		return holiday;
	}

	/**
	 * Check if this is a "no holiday" marker
	 */
	public boolean isNoHoliday() {
		return id != null && id.equals(-1L);
	}
}
