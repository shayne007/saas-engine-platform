package com.feng.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Login Demo Application
 * 
 * Demonstrates user login tracking with OpenTelemetry instrumentation.
 */
@SpringBootApplication
public class UserLoginDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserLoginDemoApplication.class, args);
    }
}
