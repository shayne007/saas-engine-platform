package com.feng.calendar.repository;

import com.feng.calendar.model.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Country entity operations
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    
    /**
     * Find country by its ISO 3166-1 alpha-2 code
     */
    Optional<Country> findByCode(String code);
    
    /**
     * Find country by name
     */
    Optional<Country> findByName(String name);
    
    /**
     * Check if country exists by code
     */
    boolean existsByCode(String code);
    
    /**
     * Find country with weekend definitions loaded
     */
    @Query("SELECT c FROM Country c LEFT JOIN FETCH c.weekendDefinitions WHERE c.code = :code")
    Optional<Country> findByCodeWithWeekendDefinitions(@Param("code") String code);
}
