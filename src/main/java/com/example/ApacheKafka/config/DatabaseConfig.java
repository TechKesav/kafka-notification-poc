package com.example.ApacheKafka.config;

import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {

    static {
        // Set the default timezone to UTC when the application starts
        // This prevents Hibernate from using deprecated timezone names
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
