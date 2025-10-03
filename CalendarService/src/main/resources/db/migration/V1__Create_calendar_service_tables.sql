-- Calendar Service Database Schema
-- Version 1: Initial schema creation

-- Countries and regions
CREATE TABLE countries (
    id SERIAL PRIMARY KEY,
    code VARCHAR(2) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    timezone_default VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Holiday definitions
CREATE TABLE holidays (
    id SERIAL PRIMARY KEY,
    country_id INTEGER NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    date DATE NOT NULL,
    holiday_type VARCHAR(50) NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_rule TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Custom business calendars
CREATE TABLE business_calendars (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    organization_id VARCHAR(50),
    country_id INTEGER NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Custom business calendar rules
CREATE TABLE business_calendar_rules (
    id SERIAL PRIMARY KEY,
    calendar_id INTEGER NOT NULL REFERENCES business_calendars(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    date DATE,
    recurrence_rule TEXT,
    description VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE
);

-- Weekend definitions per country
CREATE TABLE weekend_definitions (
    id SERIAL PRIMARY KEY,
    country_id INTEGER NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL,
    is_weekend BOOLEAN DEFAULT TRUE
);

-- Create indexes for performance
CREATE INDEX idx_holidays_country_date ON holidays (country_id, date);
CREATE INDEX idx_holidays_date ON holidays (date);
CREATE INDEX idx_holidays_country ON holidays (country_id);

CREATE INDEX idx_business_calendars_org_country ON business_calendars (organization_id, country_id);
CREATE INDEX idx_business_calendars_country ON business_calendars (country_id);

CREATE INDEX idx_business_calendar_rules_calendar_date ON business_calendar_rules (calendar_id, date);
CREATE INDEX idx_business_calendar_rules_calendar ON business_calendar_rules (calendar_id);

CREATE INDEX idx_weekend_definitions_country ON weekend_definitions (country_id);

-- Insert default countries
INSERT INTO countries (code, name, timezone_default) VALUES 
('US', 'United States', 'America/New_York'),
('GB', 'United Kingdom', 'Europe/London'),
('CA', 'Canada', 'America/Toronto'),
('AU', 'Australia', 'Australia/Sydney'),
('DE', 'Germany', 'Europe/Berlin'),
('FR', 'France', 'Europe/Paris'),
('JP', 'Japan', 'Asia/Tokyo'),
('CN', 'China', 'Asia/Shanghai'),
('IN', 'India', 'Asia/Kolkata'),
('BR', 'Brazil', 'America/Sao_Paulo');

-- Insert default weekend definitions (Saturday and Sunday for most countries)
INSERT INTO weekend_definitions (country_id, day_of_week, is_weekend)
SELECT c.id, 6, TRUE FROM countries c WHERE c.code IN ('US', 'GB', 'CA', 'AU', 'DE', 'FR', 'JP', 'CN', 'IN', 'BR');

INSERT INTO weekend_definitions (country_id, day_of_week, is_weekend)
SELECT c.id, 7, TRUE FROM countries c WHERE c.code IN ('US', 'GB', 'CA', 'AU', 'DE', 'FR', 'JP', 'CN', 'IN', 'BR');

-- Insert sample holidays for US
INSERT INTO holidays (country_id, name, date, holiday_type, is_recurring) VALUES 
((SELECT id FROM countries WHERE code = 'US'), 'New Year''s Day', '2024-01-01', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Martin Luther King Jr. Day', '2024-01-15', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Presidents'' Day', '2024-02-19', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Memorial Day', '2024-05-27', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Independence Day', '2024-07-04', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Labor Day', '2024-09-02', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Columbus Day', '2024-10-14', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Veterans Day', '2024-11-11', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Thanksgiving Day', '2024-11-28', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'US'), 'Christmas Day', '2024-12-25', 'NATIONAL', TRUE);

-- Insert sample holidays for GB
INSERT INTO holidays (country_id, name, date, holiday_type, is_recurring) VALUES 
((SELECT id FROM countries WHERE code = 'GB'), 'New Year''s Day', '2024-01-01', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Good Friday', '2024-03-29', 'RELIGIOUS', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Easter Monday', '2024-04-01', 'RELIGIOUS', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Early May Bank Holiday', '2024-05-06', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Spring Bank Holiday', '2024-05-27', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Summer Bank Holiday', '2024-08-26', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Christmas Day', '2024-12-25', 'NATIONAL', TRUE),
((SELECT id FROM countries WHERE code = 'GB'), 'Boxing Day', '2024-12-26', 'NATIONAL', TRUE);
