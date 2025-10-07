package com.feng.calendar.model.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity representing a country with its calendar configuration
 */
@Entity
@Table(name = "countries")
@Data
@EqualsAndHashCode(exclude = {"holidays", "weekendDefinitions", "businessCalendars"})
@ToString(exclude = {"holidays", "weekendDefinitions", "businessCalendars"})
public class Country {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 2, nullable = false)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "timezone_default", length = 50)
	private String timezoneDefault;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
	private List<Holiday> holidays;

	@OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
	private List<WeekendDefinition> weekendDefinitions;

	@OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
	private List<BusinessCalendar> businessCalendars;
}
