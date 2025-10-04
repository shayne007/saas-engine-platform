-- Align ID and FK column types to BIGINT to match JPA Long mappings

-- Countries
ALTER TABLE countries
    ALTER COLUMN id TYPE BIGINT;

-- Holidays
ALTER TABLE holidays
    ALTER COLUMN id TYPE BIGINT,
    ALTER COLUMN country_id TYPE BIGINT;

-- Business Calendars
ALTER TABLE business_calendars
    ALTER COLUMN id TYPE BIGINT,
    ALTER COLUMN country_id TYPE BIGINT;

-- Business Calendar Rules
ALTER TABLE business_calendar_rules
    ALTER COLUMN id TYPE BIGINT,
    ALTER COLUMN calendar_id TYPE BIGINT;

-- Weekend Definitions
ALTER TABLE weekend_definitions
    ALTER COLUMN id TYPE BIGINT,
    ALTER COLUMN country_id TYPE BIGINT;

-- Note: PostgreSQL sequences used by SERIAL are already BIGINT-capable.
-- Existing constraints and indexes will adapt to the new column types.


