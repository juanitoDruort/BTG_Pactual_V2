package btg_pactual_v1.btg_pactual_v2.application.suscripcion.query;

import btg_pactual_v1.btg_pactual_v2.builder.SuscripcionBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionesVigentesManejadorTest {

    @Mock
    private PuertoRepositorioSuscripcion repositorioSuscripcion;

    @Mock
    private PuertoRepositorioFondo repositorioFondo;

    @InjectMocks
    private SuscripcionesVigentesManejador manejador;

    @Test
    @DisplayName("manejar — retorna suscripciones activas con nombreFondo enriquecido")
    void retornaSuscripcionesConNombreFondo() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conId("sus-1").conClienteId("c1").conFondoId("fondo-1")
                .conMonto(new BigDecimal("75000")).conEstado(Suscripcion.Estado.ACTIVO).build();

        when(repositorioSuscripcion.buscarPorClienteIdYEstado("c1", Suscripcion.Estado.ACTIVO))
                .thenReturn(List.of(suscripcion));
        when(repositorioFondo.buscarPorId("fondo-1"))
                .thenReturn(Optional.of(new Fondo("fondo-1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV")));

        // Act
        List<SuscripcionVigenteResultado> resultado = manejador.manejar(new SuscripcionesVigentesConsulta("c1"));

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("sus-1", resultado.getFirst().suscripcionId());
        assertEquals("fondo-1", resultado.getFirst().fondoId());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", resultado.getFirst().nombreFondo());
        assertEquals(new BigDecimal("75000"), resultado.getFirst().monto());
        assertEquals("ACTIVO", resultado.getFirst().estado());
    }

    @Test
    @DisplayName("manejar — retorna lista vacía si no hay suscripciones activas")
    void retornaListaVacia() {
        // Arrange
        when(repositorioSuscripcion.buscarPorClienteIdYEstado("c1", Suscripcion.Estado.ACTIVO))
                .thenReturn(List.of());

        // Act
        List<SuscripcionVigenteResultado> resultado = manejador.manejar(new SuscripcionesVigentesConsulta("c1"));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("manejar — usa fondoId como fallback si fondo no existe")
    void fallbackSiFondoNoExiste() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conId("sus-1").conClienteId("c1").conFondoId("fondo-inexistente")
                .conEstado(Suscripcion.Estado.ACTIVO).build();

        when(repositorioSuscripcion.buscarPorClienteIdYEstado("c1", Suscripcion.Estado.ACTIVO))
                .thenReturn(List.of(suscripcion));
        when(repositorioFondo.buscarPorId("fondo-inexistente"))
                .thenReturn(Optional.empty());

        // Act
        List<SuscripcionVigenteResultado> resultado = manejador.manejar(new SuscripcionesVigentesConsulta("c1"));

        // Assert
        assertEquals("fondo-inexistente", resultado.getFirst().nombreFondo());
    }

    @Test
    @DisplayName("tipoDeSolicitud — retorna SuscripcionesVigentesConsulta.class")
    void tipoDeSolicitud() {
        assertEquals(SuscripcionesVigentesConsulta.class, manejador.tipoDeSolicitud());
    }
}
