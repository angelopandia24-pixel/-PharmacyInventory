package com.farmacia.infrastructure.config;

import com.farmacia.infrastructure.security.AuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuditInterceptor auditInterceptor;

    public WebConfig(AuditInterceptor auditInterceptor) {
        this.auditInterceptor = auditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Aplica el interceptor de auditoría a todas las rutas de la API
        registry.addInterceptor(auditInterceptor).addPathPatterns("/api/**");
    }
}