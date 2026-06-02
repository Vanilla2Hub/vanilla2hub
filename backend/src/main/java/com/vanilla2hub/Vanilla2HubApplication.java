package com.vanilla2hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableCaching
@ConfigurationPropertiesScan
public class Vanilla2HubApplication {

    public static void main(String[] args) {
        SpringApplication.run(Vanilla2HubApplication.class, args);
    }
}
