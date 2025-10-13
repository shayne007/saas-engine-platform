package com.feng.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Data Collector Service Application
 * 
 * This service collects telemetry data from various microservices and forwards it
 * to the storage and aggregation service using OpenTelemetry.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCollectorApplication.class, args);
    }
}
