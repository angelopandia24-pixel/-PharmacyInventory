package com.farmacia.infrastructure.security;

import com.farmacia.domain.repository.TokenBlacklistPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistPort blacklistPort;

    public JwtFilter(JwtProvider jwtProvider, TokenBlacklistPort blacklistPort) {
        this.jwtProvider = jwtProvider;
        this.blacklistPort = blacklistPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Obtener el token del encabezado HTTP
        String token = getTokenFromRequest(request);

        // 2. Validar el token usando tu JwtProvider
        if (token != null
                && !blacklistPort.estaRevocado(token)
                && jwtProvider.validateToken(token)
                && jwtProvider.isAccessToken(token)){
            String codigoAcceso = jwtProvider.getUsernameFromToken(token);
            String rol = jwtProvider.getRolFromToken(token);

            // Spring Security exige que los roles empiecen con "ROLE_" internamente
            var authority = new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rol);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(codigoAcceso, null, java.util.List.of(authority));

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

        }

        // 5. Continuar con la petición HTTP hacia su destino
        filterChain.doFilter(request, response);
    }

    // Método auxiliar para extraer el token limpito (quitando la palabra 'Bearer ')
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Corta los primeros 7 caracteres ("Bearer ")
        }
        return null;
    }
}