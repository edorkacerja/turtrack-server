package com.turtrack.server.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow your frontend origins (update with your domains as needed)
        configuration.setAllowedOrigins(Arrays.asList(
                "https://turtrack-manager-ui.vercel.app", // Production frontend
                "http://localhost:5173"                 // Local development
        ));

        // Allow standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));

        // Allow necessary headers for frontend-backend communication
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Auth-Token",
                "X-Frame-Options",
                "Referer"
        ));

        // Expose headers to frontend for custom use cases
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie", // Allow frontend to see Set-Cookie headers
                "X-Auth-Token",
                "X-XSRF-TOKEN"
        ));

        // Allow credentials for cookies and Authorization headers
        configuration.setAllowCredentials(true);

        // Cache preflight requests for a reasonable amount of time
        configuration.setMaxAge(3600L); // 1 hour

        // Apply the configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "https://turtrack-manager-ui.vercel.app",
                                "http://localhost:5173"
                        ) // Use specific domains for security
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Set-Cookie", "X-Auth-Token", "X-XSRF-TOKEN")
                        .allowCredentials(true)
                        .maxAge(3600); // Cache preflight requests for 1 hour
            }
        };
    }
}
