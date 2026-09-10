package com.dsa.ui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // 5180 = Vite dev server (frontend/vite.config.js), 5174 = published Docker port
                        .allowedOrigins(
                                "http://localhost:5180", "http://127.0.0.1:5180",
                                "http://localhost:5174", "http://127.0.0.1:5174")
                        // Vercel gives every deploy (prod + PR previews) its own *.vercel.app subdomain,
                        // so a pattern is needed here rather than one fixed origin.
                        .allowedOriginPatterns("https://*.vercel.app")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
