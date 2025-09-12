package com.feng.storage.config;

import com.feng.storage.service.api.FileStorageService;
import com.feng.storage.service.impl.FileStorageServiceImpl;
import com.feng.storage.service.impl.InMemoryFileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class FileStorageConfig {
    
    @Value("${file-storage.implementation:production}")
    private String implementation;
    
    @Bean
    @Profile("!production")
    public FileStorageService inMemoryFileStorageService() {
        return new InMemoryFileStorageService();
    }
    
    @Bean
    @Profile("production")
    @Primary
    public FileStorageService productionFileStorageService(FileStorageServiceImpl fileStorageService) {
        return fileStorageService;
    }
    
    @Bean
    @Profile("default")
    public FileStorageService defaultFileStorageService() {
        if ("in-memory".equals(implementation)) {
            return new InMemoryFileStorageService();
        } else {
            // This will be overridden by the production profile
            return new InMemoryFileStorageService();
        }
    }
}
