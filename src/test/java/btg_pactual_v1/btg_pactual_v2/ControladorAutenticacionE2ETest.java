package btg_pactual_v1.btg_pactual_v2;

import btg_pactual_v1.btg_pactual_v2.builder.RegistroComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorClienteEnMemoria;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorSuscripcionEnMemoria;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ControladorAutenticacionE2ETest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private AdaptadorClienteEnMemoria adaptadorCliente;

    @Autowired
    private AdaptadorSuscripcionEnMemoria adaptadorSuscripcion;

    private MockMvc mockMvc;

    @BeforeAll
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @BeforeEach
    void reiniciarEstado() {
        adaptadorCliente.reiniciar();
        adaptadorSuscripcion.reiniciar();
    }

    @Nested
    @DisplayName("POST /api/auth/registro")
    class RegistroUsuario {

        @Test
        @DisplayName("201 - registro exitoso con datos válidos")
        void registroExitoso() throws Exception {
            // Arrange
            String cuerpo = new RegistroComandoBuilder().buildJson();

            // Act & Assert
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.clienteId").isNotEmpty())
                    .andExpect(jsonPath("$.nombre").value("Carlos Perez"))
                    .andExpect(jsonPath("$.email").value("carlos.perez@email.com"))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"));
        }

        @Test
        @DisplayName("400 - contraseña inválida (sin caracteres especiales) → error de validación")
        void contrasenaInvalida() throws Exception {
            // Arrange
            String cuerpo = new RegistroComandoBuilder()
                    .conContrasena("abc")
                    .buildJson();

            // Act & Assert
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores").isArray());
        }

        @Test
        @DisplayName("409 - email duplicado → ExcepcionConflicto")
        void emailDuplicado() throws Exception {
            // Arrange
            String cuerpo = new RegistroComandoBuilder().buildJson();

            // Act — Primer registro — exitoso
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated());

            // Act & Assert — Segundo registro con mismo email — conflicto
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("El email ya está registrado en el sistema"));
        }

        @Test
        @DisplayName("400 - campos obligatorios faltantes → error de validación")
        void camposFaltantes() throws Exception {
            // Arrange
            String cuerpo = "{}";

            // Act & Assert
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores").isArray());
        }

        @Test
        @DisplayName("400 - formato de email inválido → error de validación")
        void emailFormatoInvalido() throws Exception {
            // Arrange
            String cuerpo = new RegistroComandoBuilder()
                    .conEmail("no-es-email")
                    .buildJson();

            // Act & Assert
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores").isArray());
        }
    }
}
