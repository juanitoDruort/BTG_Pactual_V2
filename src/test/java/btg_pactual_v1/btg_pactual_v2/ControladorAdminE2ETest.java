package btg_pactual_v1.btg_pactual_v2;

import btg_pactual_v1.btg_pactual_v2.builder.LoginComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.builder.RegistroComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Rol;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ControladorAdminE2ETest extends E2ETestBase {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String registrarYObtenerClienteId(String email, String documento) throws Exception {
        String cuerpo = new RegistroComandoBuilder()
                .conEmail(email)
                .conDocumentoIdentidad(documento)
                .buildJson();
        MvcResult resultado = mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("clienteId").asText();
    }

    private void promoverAAdministrador(String clienteId) {
        Cliente cliente = adaptadorCliente.buscarPorId(clienteId).orElseThrow();
        adaptadorCliente.guardar(new Cliente(
                cliente.getId(), cliente.getNombre(), cliente.getSaldo(),
                cliente.getEmail(), cliente.getTelefono(), cliente.getDocumentoIdentidad(),
                cliente.getContrasenaHash(), Rol.ADMINISTRADOR,
                cliente.isCuentaBloqueada(), cliente.getIntentosFallidosLogin(),
                cliente.getFechaRegistro()));
    }

    private String loginYObtenerToken(String email) throws Exception {
        String cuerpo = new LoginComandoBuilder()
                .conEmail(email)
                .buildJson();
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Nested
    @DisplayName("PUT /api/admin/usuarios/{userId}/desbloquear")
    class DesbloqueoAdmin {

        @Test
        @DisplayName("200 - desbloqueo exitoso con token JWT de ADMINISTRADOR")
        void desbloqueoExitosoConAdmin() throws Exception {
            // Arrange — registrar cliente y bloquear su cuenta
            String clienteId = registrarYObtenerClienteId("cliente@email.com", "CC111222333");
            String cuerpoIncorrecto = new LoginComandoBuilder()
                    .conEmail("cliente@email.com")
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
                            .content(new LoginComandoBuilder().conEmail("cliente@email.com").buildJson()))
                    .andExpect(status().isForbidden());

            // Registrar admin y promover a ADMINISTRADOR
            String adminId = registrarYObtenerClienteId("admin@email.com", "CC999888777");
            promoverAAdministrador(adminId);
            String tokenAdmin = loginYObtenerToken("admin@email.com");

            // Act — desbloquear cuenta con token de ADMINISTRADOR
            mockMvc.perform(put("/api/admin/usuarios/" + clienteId + "/desbloquear")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clienteId").value(clienteId))
                    .andExpect(jsonPath("$.mensaje").value("Cuenta desbloqueada exitosamente"));

            // Assert — login exitoso después del desbloqueo
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new LoginComandoBuilder().conEmail("cliente@email.com").buildJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("403 - CLIENTE intenta desbloquear cuenta → acceso denegado")
        void clienteIntentaDesbloquear() throws Exception {
            // Arrange — registrar cliente y obtener token
            registrarYObtenerClienteId("cliente@email.com", "CC111222333");
            String tokenCliente = loginYObtenerToken("cliente@email.com");

            // Act & Assert
            mockMvc.perform(put("/api/admin/usuarios/cualquier-id/desbloquear")
                            .header("Authorization", "Bearer " + tokenCliente))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("No tiene permisos para realizar esta operación"));
        }

        @Test
        @DisplayName("401 - acceso sin token a endpoint admin → no autorizado")
        void accesoSinToken() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/admin/usuarios/cualquier-id/desbloquear"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
