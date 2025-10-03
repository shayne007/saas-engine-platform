package com.feng.calendar.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<Holiday> holidays;
    
    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WeekendDefinition> weekendDefinitions;
    
    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BusinessCalendar> businessCalendars;
}
