package com.turtrack.server.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
@EntityScan(basePackages = "com.turtrack.server.model.turtrack") // Only common models
public class ProdConfig {
    // Additional prod-specific beans or configurations can go here
}