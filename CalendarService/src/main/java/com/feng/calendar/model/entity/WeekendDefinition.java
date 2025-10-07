package com.feng.calendar.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Entity defining weekend days for a specific country
 */
@Entity
@Table(name = "weekend_definitions")
@Data
@EqualsAndHashCode(exclude = "country")
@ToString(exclude = "country")
public class WeekendDefinition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "country_id", nullable = false)
	private Country country;

	@Column(name = "day_of_week", nullable = false)
	private Integer dayOfWeek; // 1=Monday, 7=Sunday

	@Column(name = "is_weekend", nullable = false)
	private Boolean isWeekend = true;
}
