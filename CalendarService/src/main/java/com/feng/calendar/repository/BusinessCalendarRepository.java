package com.feng.calendar.repository;

import com.feng.calendar.model.entity.BusinessCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BusinessCalendar entity operations
 */
@Repository
public interface BusinessCalendarRepository extends JpaRepository<BusinessCalendar, Long> {
    
    /**
     * Find business calendar by organization ID and country
     */
    @Query("SELECT bc FROM BusinessCalendar bc JOIN bc.country c " +
           "WHERE bc.organizationId = :organizationId AND c.code = :countryCode AND bc.isActive = true")
    Optional<BusinessCalendar> findByOrganizationIdAndCountryCode(@Param("organizationId") String organizationId,
                                                                  @Param("countryCode") String countryCode);
    
    /**
     * Find business calendar by ID with rules loaded
     */
    @Query("SELECT bc FROM BusinessCalendar bc LEFT JOIN FETCH bc.rules WHERE bc.id = :id AND bc.isActive = true")
    Optional<BusinessCalendar> findByIdWithRules(@Param("id") Long id);
    
    /**
     * Find all active business calendars for a country
     */
    @Query("SELECT bc FROM BusinessCalendar bc JOIN bc.country c " +
           "WHERE c.code = :countryCode AND bc.isActive = true")
    List<BusinessCalendar> findActiveByCountryCode(@Param("countryCode") String countryCode);
    
    /**
     * Find business calendar by name and organization
     */
    Optional<BusinessCalendar> findByNameAndOrganizationId(String name, String organizationId);
    
    /**
     * Check if business calendar exists by name and organization
     */
    boolean existsByNameAndOrganizationId(String name, String organizationId);
}
