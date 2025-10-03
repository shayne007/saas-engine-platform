package com.feng.calendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for Calendar Service
 * 
 * This service provides APIs to determine date types (work day, holiday, weekend)
 * and find next/previous work dates with support for multiple countries and
 * custom business calendars.
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class CalendarServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalendarServiceApplication.class, args);
    }
}
