package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.security;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionEncriptacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdaptadorEncriptacionAesTest {

    private static final String CLAVE_BASE64 = Base64.getEncoder()
            .encodeToString("BTGPactual2025AESKey32BytesXYZ!!".getBytes());

    private AdaptadorEncriptacionAes encriptacion;

    @BeforeEach
    void setUp() {
        encriptacion = new AdaptadorEncriptacionAes(CLAVE_BASE64);
    }

    @Test
    @DisplayName("encriptar y desencriptar — ciclo completo retorna dato original")
    void cicloEncriptarDesencriptar() {
        // Arrange
        String datoOriginal = "500000";

        // Act
        String encriptado = encriptacion.encriptar(datoOriginal);
        String desencriptado = encriptacion.desencriptar(encriptado);

        // Assert
        assertEquals(datoOriginal, desencriptado);
    }

    @Test
    @DisplayName("encriptar — produce resultado distinto al texto plano")
    void encriptarProduceResultadoDistinto() {
        // Arrange
        String datoOriginal = "1000000";

        // Act
        String encriptado = encriptacion.encriptar(datoOriginal);

        // Assert
        assertNotEquals(datoOriginal, encriptado);
        assertNotNull(encriptado);
    }

    @Test
    @DisplayName("encriptar — dos encriptaciones del mismo dato producen resultados diferentes (IV aleatorio)")
    void encriptarConIvAleatorio() {
        // Arrange
        String dato = "500000";

        // Act
        String encriptado1 = encriptacion.encriptar(dato);
        String encriptado2 = encriptacion.encriptar(dato);

        // Assert
        assertNotEquals(encriptado1, encriptado2);
    }

    @Test
    @DisplayName("desencriptar — dato corrupto lanza ExcepcionEncriptacion")
    void desencriptarDatoCorrupto() {
        // Arrange
        String datoCorrupto = Base64.getEncoder().encodeToString("datos-invalidos-corruptos".getBytes());

        // Act & Assert
        assertThrows(ExcepcionEncriptacion.class, () -> encriptacion.desencriptar(datoCorrupto));
    }

    @Test
    @DisplayName("desencriptar — clave diferente lanza ExcepcionEncriptacion")
    void desencriptarConClaveDiferente() {
        // Arrange
        String encriptado = encriptacion.encriptar("500000");
        String otraClave = Base64.getEncoder()
                .encodeToString("OtraClaveAES256Diferente32Bytes!".getBytes());
        AdaptadorEncriptacionAes otraInstancia = new AdaptadorEncriptacionAes(otraClave);

        // Act & Assert
        assertThrows(ExcepcionEncriptacion.class, () -> otraInstancia.desencriptar(encriptado));
    }

    @Test
    @DisplayName("constructor — clave de longitud incorrecta lanza IllegalArgumentException")
    void claveLongitudIncorrecta() {
        // Arrange
        String claveCorta = Base64.getEncoder().encodeToString("clave-corta".getBytes());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new AdaptadorEncriptacionAes(claveCorta));
    }

    @Test
    @DisplayName("encriptar y desencriptar — BigDecimal como String mantiene precisión")
    void bigDecimalComoPrecision() {
        // Arrange
        String saldoOriginal = "123456789.99";

        // Act
        String encriptado = encriptacion.encriptar(saldoOriginal);
        String desencriptado = encriptacion.desencriptar(encriptado);

        // Assert
        assertEquals(saldoOriginal, desencriptado);
    }
}
