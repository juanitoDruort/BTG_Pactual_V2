package btg_pactual_v1.btg_pactual_v2;

import btg_pactual_v1.btg_pactual_v2.builder.LoginComandoBuilder;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
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

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginUsuario {

        private void registrarUsuario() throws Exception {
            String cuerpo = new RegistroComandoBuilder().buildJson();
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("200 - login exitoso con credenciales válidas → retorna token JWT")
        void loginExitoso() throws Exception {
            // Arrange
            registrarUsuario();
            String cuerpo = new LoginComandoBuilder().buildJson();

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("401 - email inexistente → credenciales inválidas")
        void emailInexistente() throws Exception {
            // Arrange
            String cuerpo = new LoginComandoBuilder()
                    .conEmail("no.existe@email.com")
                    .buildJson();

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("401 - contraseña incorrecta → credenciales inválidas")
        void contrasenaIncorrecta() throws Exception {
            // Arrange
            registrarUsuario();
            String cuerpo = new LoginComandoBuilder()
                    .conContrasena("PasswordIncorrecto1!")
                    .buildJson();

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("403 - cuenta bloqueada tras 3 intentos fallidos → acceso denegado")
        void cuentaBloqueadaTrasIntentosFallidos() throws Exception {
            // Arrange
            registrarUsuario();
            String cuerpoIncorrecto = new LoginComandoBuilder()
                    .conContrasena("PasswordIncorrecto1!")
                    .buildJson();

            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpoIncorrecto))
                        .andExpect(status().isUnauthorized());
            }

            // Act & Assert — 4to intento debe retornar 403
            String cuerpoLogin = new LoginComandoBuilder().buildJson();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/desbloqueo/{clienteId}")
    class DesbloqueoUsuario {

        private String registrarYObtenerClienteId() throws Exception {
            String cuerpo = new RegistroComandoBuilder().buildJson();
            MvcResult resultado = mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andReturn();

            String json = resultado.getResponse().getContentAsString();
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("clienteId").asText();
        }

        private String loginYObtenerToken(String email, String contrasena) throws Exception {
            String cuerpo = new LoginComandoBuilder()
                    .conEmail(email)
                    .conContrasena(contrasena)
                    .buildJson();
            MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = resultado.getResponse().getContentAsString();
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("token").asText();
        }

        @Test
        @DisplayName("200 - desbloqueo exitoso → cuenta desbloqueada y login posterior exitoso")
        void desbloqueoExitoso() throws Exception {
            // Arrange — registrar usuario y bloquear cuenta
            String clienteId = registrarYObtenerClienteId();
            String cuerpoIncorrecto = new LoginComandoBuilder()
                    .conContrasena("PasswordIncorrecto1!")
                    .buildJson();

            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpoIncorrecto))
                        .andExpect(status().isUnauthorized());
            }

            // Verificar cuenta bloqueada
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new LoginComandoBuilder().buildJson()))
                    .andExpect(status().isForbidden());

            // Registrar un segundo usuario para obtener token de admin
            String cuerpoAdmin = new RegistroComandoBuilder()
                    .conEmail("admin@email.com")
                    .conDocumentoIdentidad("CC999888777")
                    .buildJson();
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoAdmin))
                    .andExpect(status().isCreated());
            String tokenAdmin = loginYObtenerToken("admin@email.com", "Pass1!");

            // Act — desbloquear cuenta con token autenticado
            mockMvc.perform(put("/api/auth/desbloqueo/" + clienteId)
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clienteId").value(clienteId))
                    .andExpect(jsonPath("$.mensaje").value("Cuenta desbloqueada exitosamente"));

            // Assert — login exitoso después del desbloqueo
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new LoginComandoBuilder().buildJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }
    }
}
