package com.example.javaserver.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 CORS 配置，统一处理跨域预检及实际请求。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOriginsProperty;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(resolveAllowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
            .allowedHeaders("*")
            .exposedHeaders("Authorization", "Content-Disposition")
            .allowCredentials(allowCredentials)
            .maxAge(maxAge);
    }

    @NonNull
    private String[] resolveAllowedOrigins() {
        if (!StringUtils.hasText(allowedOriginsProperty)) {
            return new String[] { "*" };
        }
        String[] origins = Arrays.stream(allowedOriginsProperty.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
        return origins.length == 0 ? new String[] { "*" } : origins;
    }
}