package com.farmacia.infrastructure.config; // capa de infraestructura, subpaquete de configuración

import com.farmacia.infrastructure.security.JwtEntryPoint; // HU-02: respuesta JSON 401
import com.farmacia.infrastructure.security.JwtFilter; // HU-02: filtro JWT
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean; // marca un método que fabrica un bean
import org.springframework.context.annotation.Configuration; // marca la clase como configuración
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus; // códigos HTTP
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // HU-02: habilita @PreAuthorize
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // constructor de reglas
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // HU-02: activa seguridad web
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // para desactivar opciones
import org.springframework.security.config.http.SessionCreationPolicy; // política de sesiones
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // HU-02: encriptador BCrypt
import org.springframework.security.crypto.password.PasswordEncoder; // HU-02: interfaz PasswordEncoder
import org.springframework.security.web.SecurityFilterChain; // cadena de filtros resultante
import org.springframework.security.web.authentication.HttpStatusEntryPoint; // responde con un código fijo
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // HU-02: filtro base de Spring
import org.springframework.web.cors.CorsConfiguration; // HU-02: configuración CORS
import org.springframework.web.cors.CorsConfigurationSource; // HU-02: fuente CORS
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // HU-02: registro de rutas CORS

import java.util.List;

@Configuration // Spring la lee al arrancar
@EnableWebSecurity // HU-02: integra la cadena de filtros de Spring Security
@EnableMethodSecurity // HU-02: permite usar @PreAuthorize en controladores
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    // HU-02: dependencias inyectadas para validar JWT y manejar errores 401
    private final JwtFilter jwtFilter;
    private final JwtEntryPoint jwtEntryPoint;

    public SecurityConfig(JwtFilter jwtFilter, JwtEntryPoint jwtEntryPoint) {
        this.jwtFilter = jwtFilter;
        this.jwtEntryPoint = jwtEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // HU-02: permite peticiones desde frontend local
                .csrf(AbstractHttpConfigurer::disable) // API con token: no se usan cookies de sesión
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // sin sesiones en el servidor
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/health").permitAll() // prueba de conexión: pública
                        .requestMatchers("/api/v1/auth/**").permitAll() // HU-02: login, refresh y logout públicos
                        .requestMatchers("/error").permitAll() // página de error interna de Spring
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").hasRole("ADMINISTRADORA")
                        .anyRequest().authenticated()) // todo lo demás exige autenticación
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jwtEntryPoint)) // HU-02: responde JSON 401 en errores de token
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // HU-02: valida el Bearer token antes de autenticar
        return http.build();
    }

    // HU-02: configuración CORS para desarrollo local
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // HU-02: bean BCrypt reutilizable para validar login y para que HU-01 hashee contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}