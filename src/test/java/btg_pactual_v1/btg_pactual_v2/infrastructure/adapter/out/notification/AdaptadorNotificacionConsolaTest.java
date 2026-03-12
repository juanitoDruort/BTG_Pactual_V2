package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.notification;

import btg_pactual_v1.btg_pactual_v2.domain.model.Notificacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AdaptadorNotificacionConsolaTest {

    private final AdaptadorNotificacionConsola adaptador = new AdaptadorNotificacionConsola();

    @Test
    @DisplayName("enviar — no lanza excepción al enviar notificación EMAIL")
    void enviarEmailNoLanzaExcepcion() {
        Notificacion notificacion = new Notificacion(
                "juan@email.com",
                Notificacion.Canal.EMAIL,
                "FPV_BTG_PACTUAL_RECAUDADORA",
                new BigDecimal("75000"),
                LocalDateTime.of(2026, 3, 12, 10, 0)
        );

        assertDoesNotThrow(() -> adaptador.enviar(notificacion));
    }

    @Test
    @DisplayName("enviar — no lanza excepción al enviar notificación SMS")
    void enviarSmsNoLanzaExcepcion() {
        Notificacion notificacion = new Notificacion(
                "3001234567",
                Notificacion.Canal.SMS,
                "DEUDAPRIVADA",
                new BigDecimal("50000"),
                LocalDateTime.of(2026, 3, 12, 10, 0)
        );

        assertDoesNotThrow(() -> adaptador.enviar(notificacion));
    }
}
