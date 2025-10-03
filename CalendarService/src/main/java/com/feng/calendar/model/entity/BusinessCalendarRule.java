package com.feng.calendar.model.entity;

import com.feng.calendar.model.enums.BusinessRuleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Entity representing a rule within a business calendar
 */
@Entity
@Table(name = "business_calendar_rules")
@Data
@EqualsAndHashCode(exclude = "businessCalendar")
@ToString(exclude = "businessCalendar")
public class BusinessCalendarRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private BusinessCalendar businessCalendar;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private BusinessRuleType ruleType;
    
    private LocalDate date;
    
    @Column(name = "recurrence_rule", columnDefinition = "TEXT")
    private String recurrenceRule;
    
    @Column(length = 200)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * Check if this rule makes the date a work day
     */
    public boolean isWorkDay() {
        return ruleType == BusinessRuleType.WORK_DAY || ruleType == BusinessRuleType.WEEKEND_OVERRIDE;
    }
}
