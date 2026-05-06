package com.johanbriger.aiservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Tillåt alla endpoints
                .allowedOrigins(
                        "http://localhost:5173", // För lokal utveckling
                        "https://funnyai-johanbriger.up.railway.app" // FRONTEND-URL PÅ RAILWAY
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}