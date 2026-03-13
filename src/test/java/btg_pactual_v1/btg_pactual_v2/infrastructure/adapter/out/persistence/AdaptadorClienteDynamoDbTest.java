package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.builder.ClienteBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoEncriptacion;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.ClienteDynamoDb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorClienteDynamoDbTest extends DynamoDbIntegrationTestBase {

    private AdaptadorClienteDynamoDb adaptador;
    private DynamoDbTable<ClienteDynamoDb> tabla;

    private final PuertoEncriptacion encriptacion = new PuertoEncriptacion() {
        @Override
        public String encriptar(String dato) { return new StringBuilder(dato).reverse().toString(); }
        @Override
        public String desencriptar(String datoEncriptado) { return new StringBuilder(datoEncriptado).reverse().toString(); }
    };

    @BeforeEach
    void setUp() {
        adaptador = new AdaptadorClienteDynamoDb(enhancedClient, encriptacion);
        tabla = enhancedClient.table("clientes", TableSchema.fromBean(ClienteDynamoDb.class));
        adaptador.reiniciar();
    }

    @Test
    @DisplayName("buscarPorId — retorna cliente guardado con saldo desencriptado")
    void buscarPorIdRetornaClienteGuardado() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conId("c1").conSaldo(new BigDecimal("500000")).build();
        adaptador.guardar(cliente);

        // Act
        Optional<Cliente> resultado = adaptador.buscarPorId("c1");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals(new BigDecimal("500000"), resultado.get().getSaldo());
        assertEquals("c1", resultado.get().getId());
    }

    @Test
    @DisplayName("guardar — persiste todos los campos incluyendo saldo encriptado")
    void guardarPersisteConSaldoEncriptado() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conId("c2")
                .conSaldo(new BigDecimal("750000"))
                .conEmail("test@test.com")
                .build();

        // Act
        adaptador.guardar(cliente);

        // Assert
        ClienteDynamoDb itemDirecto = tabla.getItem(Key.builder().partitionValue("c2").build());
        assertEquals("000057", itemDirecto.getSaldoEncriptado());
        assertEquals("test@test.com", itemDirecto.getEmail());
    }

    @Test
    @DisplayName("buscarPorEmail — retorna cliente vía GSI")
    void buscarPorEmailRetornaCliente() {
        // Arrange
        Cliente cliente = new ClienteBuilder().conId("c3").conEmail("gsi@test.com").build();
        adaptador.guardar(cliente);

        // Act
        Optional<Cliente> resultado = adaptador.buscarPorEmail("gsi@test.com");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals("c3", resultado.get().getId());
    }

    @Test
    @DisplayName("existePorEmail — retorna true si existe")
    void existePorEmailTrue() {
        // Arrange
        adaptador.guardar(new ClienteBuilder().conId("c4").conEmail("exists@test.com").build());

        // Act & Assert
        assertTrue(adaptador.existePorEmail("exists@test.com"));
    }

    @Test
    @DisplayName("existePorEmail — retorna false si no existe")
    void existePorEmailFalse() {
        // Act & Assert
        assertFalse(adaptador.existePorEmail("noexiste@test.com"));
    }

    @Test
    @DisplayName("buscarPorId — retorna Optional.empty si no existe")
    void buscarPorIdRetornaVacio() {
        // Act
        Optional<Cliente> resultado = adaptador.buscarPorId("inexistente");

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
