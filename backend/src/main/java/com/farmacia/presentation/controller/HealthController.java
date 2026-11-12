package com.farmacia.presentation.controller; // capa de presentación se encarga de recibir las peticiones HTTP y devolver respuestas HTTP
import org.springframework.web.bind.annotation.GetMapping; // mapea GET solicitudes HTTP a métodos de controlador
import org.springframework.web.bind.annotation.RequestMapping; // prefijo de ruta de la clase, en este caso /api/v1/health
import org.springframework.web.bind.annotation.RestController; // responde JSON a las solicitudes HTTP de manera automátca
import java.util.Map; // diccionario para la respuesta

@RestController // clase que responde JSON
@RequestMapping("/api/v1/health") // GET /api/v1/health si quieres probar que el backend está vivo (BRUNO O POSTMAN)
public class HealthController {
    @GetMapping // responde 200 (OK) con un JSON {"status":"UP","servicio":"farmacia-backend"}, puedes verlo en el navegador o con Postman
    public Map<String, String> health() {
        return Map.of("status", "UP", "servicio", "farmacia-backend"); // prueba de que el backend está vivo esta respuesta JSON
    }
}