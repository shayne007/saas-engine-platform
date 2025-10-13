package com.feng.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Storage Aggregation Service Application
 * 
 * This service handles storage and aggregation of telemetry data from the data collector.
 * It processes metrics, traces, and logs using OpenTelemetry and stores them in appropriate backends.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class StorageAggregationApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageAggregationApplication.class, args);
    }
}
