package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.builder.SuscripcionBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorSuscripcionEnMemoriaTest {

    private AdaptadorSuscripcionEnMemoria adaptador;

    @BeforeEach
    void setUp() {
        adaptador = new AdaptadorSuscripcionEnMemoria();
    }

    @Test
    @DisplayName("buscarPorClienteIdYEstado — retorna solo suscripciones activas del cliente")
    void filtraPorClienteIdYEstado() {
        // Arrange
        adaptador.guardar(new SuscripcionBuilder().conId("s1").conClienteId("c1").conFondoId("f1").conEstado(Suscripcion.Estado.ACTIVO).build());
        adaptador.guardar(new SuscripcionBuilder().conId("s2").conClienteId("c1").conFondoId("f2").conEstado(Suscripcion.Estado.CANCELADO).build());
        adaptador.guardar(new SuscripcionBuilder().conId("s3").conClienteId("c2").conFondoId("f1").conEstado(Suscripcion.Estado.ACTIVO).build());

        // Act
        List<Suscripcion> resultado = adaptador.buscarPorClienteIdYEstado("c1", Suscripcion.Estado.ACTIVO);

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("s1", resultado.getFirst().getId());
    }

    @Test
    @DisplayName("buscarPorClienteIdYEstado — retorna lista vacía si no hay coincidencias")
    void retornaListaVaciaSinCoincidencias() {
        // Arrange
        adaptador.guardar(new SuscripcionBuilder().conId("s1").conClienteId("c1").conEstado(Suscripcion.Estado.CANCELADO).build());

        // Act
        List<Suscripcion> resultado = adaptador.buscarPorClienteIdYEstado("c1", Suscripcion.Estado.ACTIVO);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("buscarPorClienteIdYEstado — retorna múltiples suscripciones activas del mismo cliente")
    void retornaMultiplesActivas() {
        // Arrange
        adaptador.guardar(new SuscripcionBuilder().conId("s1").conClienteId("c1").conFondoId("f1").conEstado(Suscripcion.Estado.ACTIVO).build());
        adaptador.guardar(new SuscripcionBuilder().conId("s2").conClienteId("c1").conFondoId("f2").conEstado(Suscripcion.Estado.ACTIVO).build());

        // Act
        List<Suscripcion> resultado = adaptador.buscarPorClienteIdYEstado("c1", Suscripcion.Estado.ACTIVO);

        // Assert
        assertEquals(2, resultado.size());
    }
}
