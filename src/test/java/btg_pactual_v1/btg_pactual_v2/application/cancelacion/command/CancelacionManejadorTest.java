package btg_pactual_v1.btg_pactual_v2.application.cancelacion.command;

import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoResultado;
import btg_pactual_v1.btg_pactual_v2.builder.SuscripcionBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioSuscripcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelacionManejadorTest {

    @Mock
    private IServicioSuscripcion servicioSuscripcion;

    @InjectMocks
    private CancelacionManejador manejador;

    @Test
    @DisplayName("manejar — delega a servicio y mapea resultado correctamente")
    void manejarCancelacionExitosa() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conId("sus-1")
                .conClienteId("cliente-1")
                .conFondoId("fondo-1")
                .conMonto(new BigDecimal("75000"))
                .conEstado(Suscripcion.Estado.CANCELADO)
                .build();

        when(servicioSuscripcion.cancelar("sus-1", "cliente-1")).thenReturn(suscripcion);

        // Act
        FondoResultado resultado = manejador.manejar(new CancelacionComando("sus-1", "cliente-1"));

        // Assert
        assertEquals("sus-1", resultado.suscripcionId());
        assertEquals("cliente-1", resultado.clienteId());
        assertEquals("fondo-1", resultado.fondoId());
        assertEquals(new BigDecimal("75000"), resultado.monto());
        assertEquals("CANCELADO", resultado.estado());
    }

    @Test
    @DisplayName("tipoDeSolicitud — retorna CancelacionComando.class")
    void tipoDeSolicitud() {
        // Act & Assert
        assertEquals(CancelacionComando.class, manejador.tipoDeSolicitud());
    }
}
