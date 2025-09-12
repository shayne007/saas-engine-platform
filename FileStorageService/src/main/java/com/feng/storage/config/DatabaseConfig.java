package com.feng.storage.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.feng.storage.repository")
@EntityScan(basePackages = "com.feng.storage.entity")
@EnableTransactionManagement
public class DatabaseConfig {
    // Configuration is handled by Spring Boot auto-configuration
    // Additional custom configuration can be added here if needed
}
