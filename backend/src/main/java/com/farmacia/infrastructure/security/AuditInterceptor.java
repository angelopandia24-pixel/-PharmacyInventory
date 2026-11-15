package com.farmacia.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuditInterceptor.class);

    // se ejecuta despues de terminar cada petivion HTTP
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex){
        // 1. obtener que usuario hizo la peticion
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? auth.getName()
                : "ANONIMO";

        // 2. obtener detalles de la peticion HTTP
        String metodo = request.getMethod();
        String ruta = request.getRequestURI();
        String ip = request.getRemoteAddr();
        int status = response.getStatus();

        // 3. evaluar el resultado de seguridad
        String resultado = (status >= 200 && status < 300) ? "EXITO" : "FALLO/BOQUEADO";

        // 4. Registrar el evento de auditoria
        logger.info("[AUDITORIA] Usuario: {} | Accion: {} {} | IP: {} | Estado: {} ({})",
                usuario, metodo, ruta, ip, status, resultado);

        // Registra en consola la auditoría básica de las peticiones HTTP entrantes.
    }
}
