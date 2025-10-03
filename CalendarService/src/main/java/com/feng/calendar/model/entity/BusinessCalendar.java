package com.feng.calendar.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a custom business calendar
 */
@Entity
@Table(name = "business_calendars")
@Data
@EqualsAndHashCode(exclude = {"country", "rules"})
@ToString(exclude = {"country", "rules"})
public class BusinessCalendar {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "organization_id", length = 50)
    private String organizationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "businessCalendar", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BusinessCalendarRule> rules;
}
