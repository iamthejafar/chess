package com.jafar.chess.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] resolvedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .map(this::stripQuotes)
                .map(this::removeTrailingSlash)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);

        registry.addMapping("/api/**")
                .allowedOrigins(resolvedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    private String stripQuotes(String origin) {
        if ((origin.startsWith("\"") && origin.endsWith("\""))
                || (origin.startsWith("'") && origin.endsWith("'"))) {
            return origin.substring(1, origin.length() - 1).trim();
        }
        return origin;
    }

    private String removeTrailingSlash(String origin) {
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}

