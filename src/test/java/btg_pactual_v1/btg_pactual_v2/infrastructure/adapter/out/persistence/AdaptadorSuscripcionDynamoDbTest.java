package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.builder.FondoBuilder;
import btg_pactual_v1.btg_pactual_v2.builder.SuscripcionBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.FondoDynamoDb;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.SuscripcionDynamoDb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorSuscripcionDynamoDbTest extends DynamoDbIntegrationTestBase {

    private AdaptadorSuscripcionDynamoDb adaptador;
    private DynamoDbTable<SuscripcionDynamoDb> tabla;
    private DynamoDbTable<FondoDynamoDb> tablaFondos;

    @BeforeEach
    void setUp() {
        adaptador = new AdaptadorSuscripcionDynamoDb(enhancedClient);
        tabla = enhancedClient.table("suscripciones", TableSchema.fromBean(SuscripcionDynamoDb.class));
        tablaFondos = enhancedClient.table("fondos", TableSchema.fromBean(FondoDynamoDb.class));

        adaptador.reiniciar();
        tablaFondos.scan(ScanEnhancedRequest.builder().build()).stream()
                .flatMap(page -> page.items().stream())
                .forEach(item -> tablaFondos.deleteItem(Key.builder().partitionValue(item.getId()).build()));

        tablaFondos.putItem(FondoDynamoDb.fromDomain(
                new FondoBuilder().conId("f1").conNombre("FPV_TEST").build()));
    }

    @Test
    @DisplayName("guardar — persiste suscripción con nombreFondo denormalizado")
    void guardarPersisteConNombreFondo() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder().conId("s1").conClienteId("c1").conFondoId("f1").build();

        // Act
        adaptador.guardar(suscripcion);

        // Assert
        SuscripcionDynamoDb item = tabla.getItem(Key.builder()
                .partitionValue("c1").sortValue("s1").build());
        assertEquals("FPV_TEST", item.getNombreFondo());
        assertEquals("f1", item.getFondoId());
    }

    @Test
    @DisplayName("existePorClienteIdYFondoId — retorna true para suscripción activa")
    void existeRetornaTrue() {
        // Arrange
        adaptador.guardar(new SuscripcionBuilder().conId("s1").conClienteId("c1")
                .conFondoId("f1").conEstado(Suscripcion.Estado.ACTIVO).build());

        // Act & Assert
        assertTrue(adaptador.existePorClienteIdYFondoId("c1", "f1"));
    }

    @Test
    @DisplayName("existePorClienteIdYFondoId — retorna false para suscripción cancelada")
    void existeRetornaFalseParaCancelada() {
        // Arrange
        adaptador.guardar(new SuscripcionBuilder().conId("s1").conClienteId("c1")
                .conFondoId("f1").conEstado(Suscripcion.Estado.CANCELADO).build());

        // Act & Assert
        assertFalse(adaptador.existePorClienteIdYFondoId("c1", "f1"));
    }

    @Test
    @DisplayName("buscarPorId — retorna suscripción por ID vía GSI")
    void buscarPorIdViaGSI() {
        // Arrange
        adaptador.guardar(new SuscripcionBuilder().conId("s1").conClienteId("c1").conFondoId("f1").build());

        // Act
        Optional<Suscripcion> resultado = adaptador.buscarPorId("s1");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals("s1", resultado.get().getId());
        assertEquals("c1", resultado.get().getClienteId());
    }

    @Test
    @DisplayName("buscarPorClienteIdYEstado — retorna solo activas del cliente")
    void buscarActivasDelCliente() {
        // Arrange
        adaptador.guardar(new SuscripcionBuilder().conId("s1").conClienteId("c1")
                .conFondoId("f1").conEstado(Suscripcion.Estado.ACTIVO).build());
        tablaFondos.putItem(FondoDynamoDb.fromDomain(
                new FondoBuilder().conId("f2").conNombre("FONDO_2").build()));
        adaptador.guardar(new SuscripcionBuilder().conId("s2").conClienteId("c1")
                .conFondoId("f2").conEstado(Suscripcion.Estado.CANCELADO).build());

        // Act
        List<Suscripcion> resultado = adaptador.buscarPorClienteIdYEstado("c1", Suscripcion.Estado.ACTIVO);

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("s1", resultado.getFirst().getId());
    }

    @Test
    @DisplayName("buscarPorClienteIdYEstado — retorna lista vacía si no hay coincidencias")
    void buscarRetornaVacio() {
        // Act
        List<Suscripcion> resultado = adaptador.buscarPorClienteIdYEstado("inexistente", Suscripcion.Estado.ACTIVO);

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
