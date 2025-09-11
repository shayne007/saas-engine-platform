package com.feng.storage;

import com.feng.storage.service.api.FileStorageService;
import com.feng.storage.service.impl.InMemoryFileStorageService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FileStorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileStorageServiceApplication.class, args);
    }

    @Bean
    public FileStorageService fileStorageService() {
        return new InMemoryFileStorageService();
    }
}


