package com.farmacia.infrastructure.config;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.application.usecase.RegistrarUsuario;
import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.farmacia.domain.service.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Pruebas Unitarias de UsuarioUseCaseConfig (HU-01 / SCRUM-88)")
class UsuarioUseCaseConfigTest {

    private final UsuarioRepository mockUsuarioRepository = mock(UsuarioRepository.class);
    private final PasswordHasher mockPasswordHasher = mock(PasswordHasher.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(UsuarioUseCaseConfig.class)
            .withBean(UsuarioRepository.class, () -> mockUsuarioRepository)
            .withBean(PasswordHasher.class, () -> mockPasswordHasher);

    @Test
    @DisplayName("1. Debe registrar exactamente un bean de tipo RegistrarUsuario en el contexto Spring")
    void debeRegistrarExactamenteUnBeanRegistrarUsuario() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RegistrarUsuario.class);
            assertThat(context).getBean(RegistrarUsuario.class).isNotNull();
        });
    }

    @Test
    @DisplayName("2. El bean RegistrarUsuario debe crearse inyectando UsuarioRepository y PasswordHasher")
    void debeInyectarDependenciasCorrectamente() {
        contextRunner.run(context -> {
            RegistrarUsuario registrarUsuario = context.getBean(RegistrarUsuario.class);
            assertThat(registrarUsuario).isNotNull();
        });
    }

    @Test
    @DisplayName("3. El bean RegistrarUsuario debe ser capaz de ejecutar el caso de uso con dependencias simuladas")
    void debeEjecutarCasoDeUsoCorrectamenteConDependenciasSimuladas() {
        when(mockUsuarioRepository.existsByCodigoUsuario("USR-CFG")).thenReturn(false);
        when(mockPasswordHasher.hash("ClaveSegura2026!")).thenReturn("$2a$10$hashSeguroMockGeneradoParaTest12345678901234567890123456");
        when(mockUsuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return new Usuario(101L, u.getCodigoUsuario(), u.getNombres(), u.getApellidos(), u.getPasswordHash(), u.getRol(), u.isActivo());
        });

        contextRunner.run(context -> {
            RegistrarUsuario registrarUsuario = context.getBean(RegistrarUsuario.class);

            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    "USR-CFG",
                    "Lucía",
                    "Mendoza",
                    "ClaveSegura2026!",
                    Rol.ADMINISTRADORA
            );

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(101L);
            assertThat(response.codigoUsuario()).isEqualTo("USR-CFG");
            assertThat(response.rol()).isEqualTo(Rol.ADMINISTRADORA);
        });
    }

    @Test
    @DisplayName("4. No deben registrarse adaptadores adicionales ni duplicidad de beans en el contexto")
    void noDebeGenerarDuplicidadDeBeans() {
        contextRunner.run(context -> {
            String[] beans = context.getBeanNamesForType(RegistrarUsuario.class);
            assertThat(beans).hasSize(1);
            assertThat(beans[0]).isEqualTo("registrarUsuario");
        });
    }

    @Test
    @DisplayName("5. La configuración no requiere conexión a base de datos SQL Server ni infraestructura externa")
    void configuracionNoRequiereConexionABaseDeDatos() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.containsBean("registrarUsuario")).isTrue();
        });
    }
}
