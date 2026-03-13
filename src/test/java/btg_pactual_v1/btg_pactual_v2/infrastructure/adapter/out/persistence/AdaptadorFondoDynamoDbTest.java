package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.builder.FondoBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.FondoDynamoDb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorFondoDynamoDbTest extends DynamoDbIntegrationTestBase {

    private AdaptadorFondoDynamoDb adaptador;
    private DynamoDbTable<FondoDynamoDb> tabla;

    @BeforeEach
    void setUp() {
        adaptador = new AdaptadorFondoDynamoDb(enhancedClient);
        tabla = enhancedClient.table("fondos", TableSchema.fromBean(FondoDynamoDb.class));
        tabla.scan(ScanEnhancedRequest.builder().build()).stream()
                .flatMap(page -> page.items().stream())
                .forEach(item -> tabla.deleteItem(Key.builder().partitionValue(item.getId()).build()));
    }

    @Test
    @DisplayName("buscarPorId — retorna fondo guardado")
    void buscarPorIdRetornaFondo() {
        // Arrange
        Fondo fondo = new FondoBuilder().conId("f1").conNombre("FPV_TEST").conMontoMinimo(new BigDecimal("75000")).build();
        tabla.putItem(FondoDynamoDb.fromDomain(fondo));

        // Act
        Optional<Fondo> resultado = adaptador.buscarPorId("f1");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals("FPV_TEST", resultado.get().getNombre());
        assertEquals(new BigDecimal("75000"), resultado.get().getMontoMinimo());
    }

    @Test
    @DisplayName("buscarPorId — retorna Optional.empty si no existe")
    void buscarPorIdRetornaVacio() {
        // Act
        Optional<Fondo> resultado = adaptador.buscarPorId("inexistente");

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
