package btg_pactual_v1.btg_pactual_v2;

import btg_pactual_v1.btg_pactual_v2.builder.LoginComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.builder.RegistroComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorClienteEnMemoria;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorSuscripcionEnMemoria;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ControladorSuscripcionE2ETest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private AdaptadorClienteEnMemoria adaptadorCliente;

    @Autowired
    private AdaptadorSuscripcionEnMemoria adaptadorSuscripcion;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @BeforeEach
    void reiniciarEstado() {
        adaptadorCliente.reiniciar();
        adaptadorSuscripcion.reiniciar();
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

    private String suscribirFondo(String token, String clienteId, String fondoId, int monto) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "clienteId", clienteId,
                "fondoId", fondoId,
                "monto", monto
        ));

        MvcResult resultado = mockMvc.perform(post("/api/fondos/suscribir")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("suscripcionId").asText();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/suscripciones/{id}/cancelar
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/suscripciones/{id}/cancelar")
    class CancelarSuscripcion {

        @Test
        @DisplayName("200 - cancelación exitosa de suscripción activa")
        void cancelacionExitosa() throws Exception {
            // Arrange
            String clienteId = registrarYObtenerClienteId("cancel@email.com", "CC111222333");
            String token = loginYObtenerToken("cancel@email.com");
            String suscripcionId = suscribirFondo(token, clienteId, "fondo-1", 75000);

            // Act & Assert
            mockMvc.perform(post("/api/suscripciones/" + suscripcionId + "/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suscripcionId").value(suscripcionId))
                    .andExpect(jsonPath("$.clienteId").value(clienteId))
                    .andExpect(jsonPath("$.fondoId").value("fondo-1"))
                    .andExpect(jsonPath("$.monto").value(75000))
                    .andExpect(jsonPath("$.estado").value("CANCELADO"))
                    .andExpect(jsonPath("$.fechaSuscripcion").isNotEmpty());
        }

        @Test
        @DisplayName("422 - suscripción no encontrada → ExcepcionDominio")
        void suscripcionNoEncontrada() throws Exception {
            // Arrange
            String clienteId = registrarYObtenerClienteId("notfound@email.com", "CC111222333");
            String token = loginYObtenerToken("notfound@email.com");

            // Act & Assert
            mockMvc.perform(post("/api/suscripciones/sus-inexistente/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("Suscripción no encontrada: sus-inexistente"));
        }

        @Test
        @DisplayName("422 - suscripción ya cancelada → ExcepcionDominio con nombre del fondo")
        void suscripcionYaCancelada() throws Exception {
            // Arrange
            String clienteId = registrarYObtenerClienteId("doblecancel@email.com", "CC111222333");
            String token = loginYObtenerToken("doblecancel@email.com");
            String suscripcionId = suscribirFondo(token, clienteId, "fondo-1", 75000);

            // Primera cancelación — exitosa
            mockMvc.perform(post("/api/suscripciones/" + suscripcionId + "/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Act & Assert — segunda cancelación — rechazada
            mockMvc.perform(post("/api/suscripciones/" + suscripcionId + "/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(
                        "No se puede cancelar el FPV_BTG_PACTUAL_RECAUDADORA ya que no esta activo dentro de su portafolio"
                    ));
        }

        @Test
        @DisplayName("200 - verificación devolución de saldo (AC3: 425.000 + 75.000 = 500.000)")
        void verificacionDevolucionSaldo() throws Exception {
            // Arrange — cliente con saldo inicial $500.000
            String clienteId = registrarYObtenerClienteId("saldo@email.com", "CC111222333");
            String token = loginYObtenerToken("saldo@email.com");

            // Suscribir por $75.000 → saldo queda en $425.000
            String suscripcionId = suscribirFondo(token, clienteId, "fondo-1", 75000);

            // Act — cancelar → saldo debe volver a $500.000
            mockMvc.perform(post("/api/suscripciones/" + suscripcionId + "/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADO"));

            // Assert — re-suscribir por $500.000 al fondo-4 ($250.000 mínimo) demuestra saldo restaurado
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", clienteId,
                    "fondoId", "fondo-4",
                    "monto", 500000
            ));
            mockMvc.perform(post("/api/fondos/suscribir")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.monto").value(500000));
        }

        @Test
        @DisplayName("200 - cancelación de todas las suscripciones (AC4)")
        void cancelarTodasSuscripciones() throws Exception {
            // Arrange
            String clienteId = registrarYObtenerClienteId("todas@email.com", "CC111222333");
            String token = loginYObtenerToken("todas@email.com");

            String sus1 = suscribirFondo(token, clienteId, "fondo-1", 75000);
            String sus2 = suscribirFondo(token, clienteId, "fondo-3", 50000);

            // Act & Assert — cancelar ambas
            mockMvc.perform(post("/api/suscripciones/" + sus1 + "/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADO"));

            mockMvc.perform(post("/api/suscripciones/" + sus2 + "/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADO"));
        }

        @Test
        @DisplayName("200 - re-suscripción al mismo fondo tras cancelación (AC5)")
        void reSuscripcionTrasCancelacion() throws Exception {
            // Arrange
            String clienteId = registrarYObtenerClienteId("resub@email.com", "CC111222333");
            String token = loginYObtenerToken("resub@email.com");

            String suscripcionId = suscribirFondo(token, clienteId, "fondo-1", 75000);

            // Cancelar
            mockMvc.perform(post("/api/suscripciones/" + suscripcionId + "/cancelar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Act & Assert — re-suscribir al mismo fondo
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", clienteId,
                    "fondoId", "fondo-1",
                    "monto", 75000
            ));
            mockMvc.perform(post("/api/fondos/suscribir")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fondoId").value("fondo-1"))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("401 - acceso sin token → no autorizado")
        void accesoSinToken() throws Exception {
            mockMvc.perform(post("/api/suscripciones/cualquier-id/cancelar"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - cancelar suscripción de otro cliente → acceso denegado")
        void cancelarSuscripcionDeOtroCliente() throws Exception {
            // Arrange — cliente A suscribe
            String clienteIdA = registrarYObtenerClienteId("clienteA@email.com", "CC111000111");
            String tokenA = loginYObtenerToken("clienteA@email.com");
            String suscripcionId = suscribirFondo(tokenA, clienteIdA, "fondo-1", 75000);

            // Cliente B intenta cancelar la suscripción de A
            String clienteIdB = registrarYObtenerClienteId("clienteB@email.com", "CC222000222");
            String tokenB = loginYObtenerToken("clienteB@email.com");

            // Act & Assert
            mockMvc.perform(post("/api/suscripciones/" + suscripcionId + "/cancelar")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("No tiene permisos para cancelar esta suscripción"));
        }
    }
}
