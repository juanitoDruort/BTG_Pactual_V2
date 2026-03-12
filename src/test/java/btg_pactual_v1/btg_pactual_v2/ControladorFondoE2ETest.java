package btg_pactual_v1.btg_pactual_v2;

import btg_pactual_v1.btg_pactual_v2.builder.LoginComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.builder.RegistroComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Rol;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorClienteEnMemoria;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorSuscripcionEnMemoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
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
    @WithMockUser(username = "cliente-1", roles = "CLIENTE")
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
        @WithMockUser(username = "cliente-inexistente", roles = "CLIENTE")
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
                            "No tiene saldo disponible para vincularse al fondo FDO-ACCIONES. Saldo actual: 500000"
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
    @WithMockUser
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

    // ──────────────────────────────────────────────────────────────────────────
    // Aislamiento de Datos — clienteId del JWT
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Aislamiento de datos — clienteId resuelto del JWT")
    class AislamientoDatos {

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

        @Test
        @DisplayName("201 - CLIENTE suscribe fondo propio → clienteId del JWT")
        void clienteSuscribeFondoPropio() throws Exception {
            // Arrange
            String clienteId = registrarYObtenerClienteId("cliente.propio@email.com", "CC111222333");
            String token = loginYObtenerToken("cliente.propio@email.com");

            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", clienteId,
                    "fondoId", "fondo-1",
                    "monto", 75000
            ));

            // Act & Assert
            mockMvc.perform(post("/api/fondos/suscribir")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.clienteId").value(clienteId))
                    .andExpect(jsonPath("$.fondoId").value("fondo-1"));
        }

        @Test
        @DisplayName("201 - CLIENTE envía clienteId de otro en body → sistema usa JWT (prevención BOLA)")
        void clienteEnviaIdDeOtroPeroSistemaUsaJwt() throws Exception {
            // Arrange — registrar dos usuarios
            String clienteIdA = registrarYObtenerClienteId("userA@email.com", "CC111000111");
            String clienteIdB = registrarYObtenerClienteId("userB@email.com", "CC222000222");
            String tokenA = loginYObtenerToken("userA@email.com");

            // Body con clienteId de B, pero token de A
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", clienteIdB,
                    "fondoId", "fondo-1",
                    "monto", 75000
            ));

            // Act & Assert — suscripción se crea para A (JWT), NO para B (body ignorado)
            mockMvc.perform(post("/api/fondos/suscribir")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.clienteId").value(clienteIdA));
        }

        @Test
        @DisplayName("201 - ADMINISTRADOR suscribe fondo en nombre de cliente → usa clienteId del body")
        void adminSuscribeEnNombreDeCliente() throws Exception {
            // Arrange — registrar cliente
            String clienteId = registrarYObtenerClienteId("cliente@email.com", "CC111222333");

            // Registrar admin y promover
            String adminId = registrarYObtenerClienteId("admin@email.com", "CC999888777");
            promoverAAdministrador(adminId);
            String tokenAdmin = loginYObtenerToken("admin@email.com");

            // Body con clienteId del cliente
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", clienteId,
                    "fondoId", "fondo-1",
                    "monto", 75000
            ));

            // Act & Assert — suscripción se crea para el cliente (body usado para ADMIN)
            mockMvc.perform(post("/api/fondos/suscribir")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.clienteId").value(clienteId));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Protección JWT de endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Protección JWT — endpoints protegidos")
    class ProteccionJWT {

        private String obtenerTokenValido() throws Exception {
            String cuerpoRegistro = new RegistroComandoBuilder()
                    .conEmail("jwt.test@email.com")
                    .conDocumentoIdentidad("CC555666777")
                    .buildJson();
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRegistro))
                    .andExpect(status().isCreated());

            String cuerpoLogin = new LoginComandoBuilder()
                    .conEmail("jwt.test@email.com")
                    .buildJson();
            var resultado = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin))
                    .andExpect(status().isOk())
                    .andReturn();

            return new ObjectMapper().readTree(resultado.getResponse().getContentAsString())
                    .get("token").asText();
        }

        @Test
        @DisplayName("200 - acceso a endpoint protegido con token JWT válido")
        void accesoConTokenValido() throws Exception {
            // Arrange
            String token = obtenerTokenValido();

            // Act & Assert
            mockMvc.perform(get("/api/fondos/fondo-1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("fondo-1"));
        }

        @Test
        @DisplayName("401 - acceso sin token (cabecera Authorization ausente)")
        void accesoSinToken() throws Exception {
            mockMvc.perform(get("/api/fondos/fondo-1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - acceso con token malformado")
        void accesoConTokenMalformado() throws Exception {
            mockMvc.perform(get("/api/fondos/fondo-1")
                            .header("Authorization", "Bearer token.invalido.malformado"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - acceso con token expirado")
        void accesoConTokenExpirado() throws Exception {
            // Arrange — generar token con expiración en el pasado usando JJWT directamente
            javax.crypto.SecretKey clave = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                    "S3cur3K3yBTGPactual2025!xYz9Km4QwErTyU1OpAsD".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String tokenExpirado = io.jsonwebtoken.Jwts.builder()
                    .subject("cliente-1")
                    .claim("rol", "CLIENTE")
                    .issuedAt(new java.util.Date(System.currentTimeMillis() - 600000))
                    .expiration(new java.util.Date(System.currentTimeMillis() - 300000))
                    .signWith(clave)
                    .compact();

            // Act & Assert
            mockMvc.perform(get("/api/fondos/fondo-1")
                            .header("Authorization", "Bearer " + tokenExpirado))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Notificación tras suscripción (fire-and-forget)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Notificación tras suscripción")
    @ExtendWith(OutputCaptureExtension.class)
    @WithMockUser(username = "cliente-1", roles = "CLIENTE")
    class NotificacionSuscripcion {

        @Test
        @DisplayName("201 - suscripción exitosa genera notificación en consola")
        void suscripcionExitosaGeneraNotificacion(CapturedOutput output) throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-1",
                    "monto",     75000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isCreated());

            assertThat(output.getAll()).contains("[NOTIFICACION-EMAIL]");
            assertThat(output.getAll()).contains("FPV_BTG_PACTUAL_RECAUDADORA");
        }

        @Test
        @DisplayName("422 - suscripción fallida NO genera notificación")
        void suscripcionFallidaNoGeneraNotificacion(CapturedOutput output) throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "clienteId", "cliente-1",
                    "fondoId",   "fondo-1",
                    "monto",     50000
            ));

            mockMvc.perform(post("/api/fondos/suscribir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isUnprocessableEntity());

            assertThat(output.getAll()).doesNotContain("[NOTIFICACION-EMAIL]");
        }
    }
}
