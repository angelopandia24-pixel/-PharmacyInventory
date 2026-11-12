package com.farmacia.infrastructure.config; // capa de infraestructura, subpaquete de configuración
import org.springframework.context.annotation.Bean; // marca un método que fabrica un bean
import org.springframework.context.annotation.Configuration; // marca la clase como configuración
import org.springframework.http.HttpStatus; // códigos HTTP
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // constructor de reglas
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // para desactivar opciones
import org.springframework.security.config.http.SessionCreationPolicy; // política de sesiones
import org.springframework.security.web.SecurityFilterChain; // cadena de filtros resultante
import org.springframework.security.web.authentication.HttpStatusEntryPoint; // responde con un código fijo

@Configuration // Spring la lee al arrancar
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // API con token: no se usan cookies de sesión
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // sin sesiones en el servidor
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/health").permitAll() // prueba de conexión: pública
                        .requestMatchers("/error").permitAll() // página de error interna de Spring
                        .anyRequest().authenticated()) // todo lo demás exige autenticación
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))); // sin acceso: 401, no el formulario de login
        return http.build();
    }
}