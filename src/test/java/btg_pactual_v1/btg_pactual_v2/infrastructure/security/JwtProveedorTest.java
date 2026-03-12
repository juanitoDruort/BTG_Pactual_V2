package btg_pactual_v1.btg_pactual_v2.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProveedorTest {

    private static final String SECRETO = "S3cur3K3yBTGPactual2025!xYz9Km4QwErTyU1OpAsD";
    private static final long EXPIRACION_MS = 300000;

    private JwtProveedor jwtProveedor;

    @BeforeEach
    void setUp() {
        jwtProveedor = new JwtProveedor(SECRETO, EXPIRACION_MS);
    }

    @Test
    @DisplayName("generarToken — contiene claims correctos (sub, rol, exp)")
    void generarTokenConClaimsCorrectos() {
        // Act
        String token = jwtProveedor.generarToken("cliente-1", "CLIENTE");

        // Assert
        assertEquals("cliente-1", jwtProveedor.extraerUserId(token));
        assertEquals("CLIENTE", jwtProveedor.extraerRol(token));
    }

    @Test
    @DisplayName("validarToken — token válido retorna true")
    void validarTokenValido() {
        // Arrange
        String token = jwtProveedor.generarToken("cliente-1", "CLIENTE");

        // Act & Assert
        assertTrue(jwtProveedor.validarToken(token));
    }

    @Test
    @DisplayName("validarToken — token con firma inválida retorna false")
    void validarTokenFirmaInvalida() {
        // Arrange
        SecretKey otraClave = Keys.hmacShaKeyFor("OtraClaveSecretaDiferenteXyz1234567890!!".getBytes(StandardCharsets.UTF_8));
        String tokenConOtraFirma = Jwts.builder()
                .subject("cliente-1")
                .claim("rol", "CLIENTE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(otraClave)
                .compact();

        // Act & Assert
        assertFalse(jwtProveedor.validarToken(tokenConOtraFirma));
    }

    @Test
    @DisplayName("validarToken — token expirado retorna false")
    void validarTokenExpirado() {
        // Arrange
        JwtProveedor proveedorExpirado = new JwtProveedor(SECRETO, -1000);
        String tokenExpirado = proveedorExpirado.generarToken("cliente-1", "CLIENTE");

        // Act & Assert
        assertFalse(jwtProveedor.validarToken(tokenExpirado));
    }

    @Test
    @DisplayName("extraerUserId y extraerRol — extrae datos correctamente de token válido")
    void extraerDatosDeToken() {
        // Arrange
        String token = jwtProveedor.generarToken("admin-1", "ADMINISTRADOR");

        // Act
        String userId = jwtProveedor.extraerUserId(token);
        String rol = jwtProveedor.extraerRol(token);

        // Assert
        assertEquals("admin-1", userId);
        assertEquals("ADMINISTRADOR", rol);
    }
}
