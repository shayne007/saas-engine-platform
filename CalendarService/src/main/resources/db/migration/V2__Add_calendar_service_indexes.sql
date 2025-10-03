-- Calendar Service Database Schema
-- Version 2: Additional indexes for performance optimization

-- Optimized indexes for common queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_holidays_country_date_range 
ON holidays (country_id, date) 
WHERE date >= CURRENT_DATE - INTERVAL '1 year' 
AND date <= CURRENT_DATE + INTERVAL '2 years';

-- Partial index for active business calendars
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_business_calendars_active 
ON business_calendars (organization_id, country_id) 
WHERE is_active = true;

-- Partial index for active business calendar rules
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_business_calendar_rules_active 
ON business_calendar_rules (calendar_id, date) 
WHERE is_active = true;

-- Index for holiday type queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_holidays_type 
ON holidays (holiday_type, date);

-- Index for recurring holidays
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_holidays_recurring 
ON holidays (country_id, is_recurring, date) 
WHERE is_recurring = true;

-- Index for business calendar rules by type
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_business_calendar_rules_type 
ON business_calendar_rules (calendar_id, rule_type, date) 
WHERE is_active = true;
