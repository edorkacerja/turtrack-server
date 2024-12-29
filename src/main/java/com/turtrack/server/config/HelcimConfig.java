package com.turtrack.server.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "helcim")
@Data
public class HelcimConfig {

    private String apiToken;
    private String baseUrl;
    private String accountId;

}
