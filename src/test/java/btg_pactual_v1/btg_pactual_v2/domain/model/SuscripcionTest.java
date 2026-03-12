package btg_pactual_v1.btg_pactual_v2.domain.model;

import btg_pactual_v1.btg_pactual_v2.builder.SuscripcionBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuscripcionTest {

    @Test
    @DisplayName("cancelar — suscripción ACTIVA cambia a CANCELADO y retorna monto")
    void cancelarSuscripcionActiva() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conEstado(Suscripcion.Estado.ACTIVO)
                .conMonto(new BigDecimal("75000"))
                .build();

        // Act
        BigDecimal montoDevuelto = suscripcion.cancelar("FPV_BTG_PACTUAL_RECAUDADORA");

        // Assert
        assertEquals(Suscripcion.Estado.CANCELADO, suscripcion.getEstado());
        assertEquals(new BigDecimal("75000"), montoDevuelto);
    }

    @Test
    @DisplayName("cancelar — suscripción CANCELADA lanza ExcepcionDominio con nombre del fondo")
    void cancelarSuscripcionYaCancelada() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conEstado(Suscripcion.Estado.CANCELADO)
                .build();

        // Act & Assert
        ExcepcionDominio ex = assertThrows(ExcepcionDominio.class,
                () -> suscripcion.cancelar("FPV_BTG_PACTUAL_RECAUDADORA"));
        assertEquals(
            "No se puede cancelar el FPV_BTG_PACTUAL_RECAUDADORA ya que no esta activo dentro de su portafolio",
            ex.getMessage()
        );
    }
}
