package btg_pactual_v1.btg_pactual_v2;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests E2E: levanta el contexto completo de Spring una sola vez.
 * El estado de los adaptadores en memoria se reinicia en @BeforeEach
 * para garantizar aislamiento entre tests sin reiniciar el contexto.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ControladorFondoE2ETest {

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
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @BeforeEach
    void reiniciarEstado() {
        adaptadorCliente.reiniciar();
        adaptadorSuscripcion.reiniciar();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/fondos/suscribir
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/fondos/suscribir")
    class SuscribirFondo {

        @Test
        @DisplayName("201 - suscripción exitosa con datos válidos")
        void suscripcionExitosa() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-1",
                    "monto",     75000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.suscripcionId").isNotEmpty())
                    .andExpect(jsonPath("$.clienteId").value("cliente-1"))
                    .andExpect(jsonPath("$.fondoId").value("fondo-1"))
                    .andExpect(jsonPath("$.monto").value(75000))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"))
                    .andExpect(jsonPath("$.fechaSuscripcion").isNotEmpty());
        }

        @Test
        @DisplayName("400 - clienteId vacío → error de validación")
        void clienteIdVacio() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "",
                    "fondoId",   "fondo-1",
                    "monto",     75000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores").isArray())
                    .andExpect(jsonPath("$.errores[0]").value("clienteId: El clienteId es obligatorio"));
        }

        @Test
        @DisplayName("400 - fondoId vacío → error de validación")
        void fondoIdVacio() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "",
                    "monto",     75000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores").isArray())
                    .andExpect(jsonPath("$.errores[0]").value("fondoId: El fondoId es obligatorio"));
        }

        @Test
        @DisplayName("400 - monto negativo → error de validación")
        void montoNegativo() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-1",
                    "monto",     -1000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores").isArray());
        }

        @Test
        @DisplayName("422 - fondo no existe → ExcepcionDominio")
        void fondoNoExiste() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-inexistente",
                    "monto",     75000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("Fondo no encontrado: fondo-inexistente"));
        }

        @Test
        @DisplayName("422 - cliente no existe → ExcepcionDominio")
        void clienteNoExiste() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-inexistente",
                    "fondoId",   "fondo-1",
                    "monto",     75000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("Cliente no encontrado: cliente-inexistente"));
        }

        @Test
        @DisplayName("422 - monto menor al mínimo del fondo → ExcepcionDominio")
        void montoMenorAlMinimo() throws Exception {
            // fondo-1 mínimo $75.000 — enviamos $50.000
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-1",
                    "monto",     50000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(
                            "El monto mínimo para vincularse al fondo FPV_BTG_PACTUAL_RECAUDADORA es $75000"
                    ));
        }

        @Test
        @DisplayName("422 - saldo insuficiente → ExcepcionDominio")
        void saldoInsuficiente() throws Exception {
            // cliente-1 saldo $500.000 — fondo-4 mínimo $250.000 — enviamos $600.000
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-4",
                    "monto",     600000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(
                            "No tiene saldo disponible para vincularse al fondo. Saldo actual: 500000"
                    ));
        }

        @Test
        @DisplayName("422 - cliente ya vinculado al mismo fondo → ExcepcionDominio")
        void clienteYaSuscrito() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-1",
                    "monto",     75000
            ));

            // Primera suscripción — exitosa
            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated());

            // Segunda suscripción al mismo fondo — rechazada
            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value(
                            "El cliente ya está vinculado al fondo FPV_BTG_PACTUAL_RECAUDADORA"
                    ));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/fondos/{fondoId}
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/fondos/{fondoId}")
    class ObtenerFondo {

        @Test
        @DisplayName("200 - fondo existe → retorna datos del fondo")
        void fondoExiste() throws Exception {
            mockMvc.perform(get("/api/fondos/fondo-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("fondo-1"))
                    .andExpect(jsonPath("$.nombre").value("FPV_BTG_PACTUAL_RECAUDADORA"))
                    .andExpect(jsonPath("$.montoMinimo").value(75000))
                    .andExpect(jsonPath("$.categoria").value("FPV"));
        }

        @Test
        @DisplayName("200 - todos los fondos precargados son consultables")
        void todosFondosExisten() throws Exception {
            for (int i = 1; i <= 5; i++) {
                mockMvc.perform(get("/api/fondos/fondo-" + i))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("fondo-" + i));
            }
        }

        @Test
        @DisplayName("422 - fondo no existe → ExcepcionDominio")
        void fondoNoExiste() throws Exception {
            mockMvc.perform(get("/api/fondos/fondo-inexistente"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("Fondo no encontrado: fondo-inexistente"));
        }
    }
}
