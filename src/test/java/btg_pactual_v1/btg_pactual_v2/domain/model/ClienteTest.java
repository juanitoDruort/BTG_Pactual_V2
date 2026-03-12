package btg_pactual_v1.btg_pactual_v2.domain.model;

import btg_pactual_v1.btg_pactual_v2.builder.ClienteBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteTest {

    @Test
    @DisplayName("incrementarIntentosFallidos — incrementa el contador en 1")
    void incrementarIntentosFallidos() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conIntentosFallidosLogin(0).build();

        // Act
        cliente.incrementarIntentosFallidos();

        // Assert
        assertEquals(1, cliente.getIntentosFallidosLogin());
    }

    @Test
    @DisplayName("bloquearCuenta — marca la cuenta como bloqueada")
    void bloquearCuenta() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conCuentaBloqueada(false).build();

        // Act
        cliente.bloquearCuenta();

        // Assert
        assertTrue(cliente.estaBloqueada());
    }

    @Test
    @DisplayName("resetearIntentosFallidos — lleva el contador a cero")
    void resetearIntentosFallidos() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conIntentosFallidosLogin(2).build();

        // Act
        cliente.resetearIntentosFallidos();

        // Assert
        assertEquals(0, cliente.getIntentosFallidosLogin());
    }

    @Test
    @DisplayName("estaBloqueada — retorna true cuando la cuenta está bloqueada")
    void estaBloqueadaTrue() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conCuentaBloqueada(true).build();

        // Act & Assert
        assertTrue(cliente.estaBloqueada());
    }

    @Test
    @DisplayName("estaBloqueada — retorna false cuando la cuenta no está bloqueada")
    void estaBloqueadaFalse() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conCuentaBloqueada(false).build();

        // Act & Assert
        assertFalse(cliente.estaBloqueada());
    }

    @Test
    @DisplayName("desbloquearCuenta — desbloquea y resetea intentos fallidos")
    void desbloquearCuenta() {
        // Arrange
        Cliente cliente = new ClienteBuilder()
                .conCuentaBloqueada(true)
                .conIntentosFallidosLogin(3)
                .build();

        // Act
        cliente.desbloquearCuenta();

        // Assert
        assertFalse(cliente.estaBloqueada());
        assertEquals(0, cliente.getIntentosFallidosLogin());
    }
}
