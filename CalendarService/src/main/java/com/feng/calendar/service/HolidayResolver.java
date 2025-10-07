package com.feng.calendar.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.feng.calendar.model.entity.Holiday;
import com.feng.calendar.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for resolving holidays with multi-level caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayResolver {

	private final HolidayRepository holidayRepository;

	private final RedisTemplate<String, Object> redisTemplate;

	private final ExternalHolidayClient externalHolidayClient;

	private static final String HOLIDAY_CACHE_KEY = "holiday:%s:%s"; // country:date

	private static final Duration CACHE_TTL = Duration.ofHours(24);

	/**
	 * Find holiday for a specific date and country
	 */
	public Optional<Holiday> findHoliday(LocalDate date, String countryCode) {
		String cacheKey = String.format(HOLIDAY_CACHE_KEY, countryCode, date.toString());

		// Try L1 cache (Redis) first
		Object cachedObject = getCachedObject(cacheKey);
		if (cachedObject != null) {
			// Handle both Holiday objects and LinkedHashMap (for backward compatibility)
			Holiday cachedHoliday = convertToHoliday(cachedObject);
			if (cachedHoliday != null) {
				return cachedHoliday.isNoHoliday() ? Optional.empty() :
						Optional.of(cachedHoliday);
			}
		}

		// Check database
		Optional<Holiday> holiday =
				holidayRepository.findByCountryCodeAndDate(countryCode, date);

		if (holiday.isPresent()) {
			cacheHoliday(cacheKey, holiday.get());
			return holiday;
		}

		// Check external APIs for real-time data
		try {
			Optional<Holiday> externalHoliday =
					externalHolidayClient.fetchHoliday(date, countryCode);

			if (externalHoliday.isPresent()) {
				// Save to database for future use
				Holiday savedHoliday = holidayRepository.save(externalHoliday.get());
				cacheHoliday(cacheKey, savedHoliday);
				return Optional.of(savedHoliday);
			}
		}
		catch (Exception e) {
			log.warn(
					"Failed to fetch holiday from external API for date {} in country " +
							"{}",
					date, countryCode, e);
		}

		// Cache negative result
		Holiday noHoliday = Holiday.noHoliday();
		cacheHoliday(cacheKey, noHoliday);

		return Optional.empty();
	}

	/**
	 * Cache holiday result
	 */
	private void cacheHoliday(String cacheKey, Holiday holiday) {
		try {
			redisTemplate.opsForValue().set(cacheKey, holiday, CACHE_TTL);
		}
		catch (Exception e) {
			log.warn("Failed to cache holiday for key: {}", cacheKey, e);
		}
	}

	/**
	 * Clear holiday cache for a specific date and country
	 */
	public void clearHolidayCache(LocalDate date, String countryCode) {
		String cacheKey = String.format(HOLIDAY_CACHE_KEY, countryCode, date.toString());
		redisTemplate.delete(cacheKey);
	}

	/**
	 * Clear all holiday cache for a country
	 */
	public void clearHolidayCacheForCountry(String countryCode) {
		String pattern =
				String.format(HOLIDAY_CACHE_KEY.replace(":%s", ""), countryCode) + ":*";
		redisTemplate.delete(redisTemplate.keys(pattern));
	}

	/**
	 * Clear all holiday cache (useful for fixing serialization issues)
	 */
	public void clearAllHolidayCache() {
		String pattern = "holiday:*";
		redisTemplate.delete(redisTemplate.keys(pattern));
		log.info("Cleared all holiday cache");
	}

	/**
	 * Convert cached object to Holiday, handling both Holiday objects and LinkedHashMap
	 */
	private Holiday convertToHoliday(Object cachedObject) {
		if (cachedObject instanceof Holiday) {
			return (Holiday) cachedObject;
		}

		if (cachedObject instanceof LinkedHashMap) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) cachedObject;

				// Check if this is a "no holiday" marker
				Object id = map.get("id");
				if (id != null && id.equals(-1L)) {
					return Holiday.noHoliday();
				}

				// For regular holidays, we need to reconstruct from the map
				// This is a fallback for existing cached data
				log.warn(
						"Found LinkedHashMap in cache, this indicates serialization " +
								"issue. " +
								"Consider clearing cache or updating serialization " +
								"configuration.");

				// Return null to force database lookup
				return null;
			}
			catch (Exception e) {
				log.warn("Failed to convert LinkedHashMap to Holiday", e);
				return null;
			}
		}

		log.warn("Unexpected cached object type: {}", cachedObject.getClass());
		return null;
	}

	/**
	 * Safely get object from Redis cache with error handling
	 */
	private Object getCachedObject(String cacheKey) {
		try {
			return redisTemplate.opsForValue().get(cacheKey);
		}
		catch (Exception e) {
			log.warn("Failed to retrieve cached object for key: {}", cacheKey, e);
			// Clear the problematic cache entry
			redisTemplate.delete(cacheKey);
			return null;
		}
	}
}