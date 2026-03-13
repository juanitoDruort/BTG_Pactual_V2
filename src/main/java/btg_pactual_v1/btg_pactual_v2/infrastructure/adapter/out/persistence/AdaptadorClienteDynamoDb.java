package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoEncriptacion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.ClienteDynamoDb;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AdaptadorClienteDynamoDb implements PuertoRepositorioCliente {

    private final DynamoDbTable<ClienteDynamoDb> tabla;
    private final DynamoDbIndex<ClienteDynamoDb> emailIndex;
    private final PuertoEncriptacion encriptacion;

    public AdaptadorClienteDynamoDb(DynamoDbEnhancedClient enhancedClient,
                                    PuertoEncriptacion encriptacion) {
        this.tabla = enhancedClient.table("clientes", TableSchema.fromBean(ClienteDynamoDb.class));
        this.emailIndex = tabla.index("email-index");
        this.encriptacion = encriptacion;
    }

    @Override
    public Optional<Cliente> buscarPorId(String id) {
        ClienteDynamoDb item = tabla.getItem(Key.builder().partitionValue(id).build());
        if (item == null) {
            return Optional.empty();
        }
        return Optional.of(reconstruirConSaldoDesencriptado(item));
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteDynamoDb item = ClienteDynamoDb.fromDomain(cliente);
        item.setSaldoEncriptado(encriptacion.encriptar(cliente.getSaldo().toPlainString()));
        tabla.putItem(item);
        return cliente;
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(email).build()))
                .limit(1)
                .build();

        return emailIndex.query(request).stream()
                .flatMap(page -> page.items().stream())
                .findFirst()
                .map(this::reconstruirConSaldoDesencriptado);
    }

    @Override
    public boolean existePorEmail(String email) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(email).build()))
                .limit(1)
                .build();

        return emailIndex.query(request).stream()
                .anyMatch(page -> !page.items().isEmpty());
    }

    public void reiniciar() {
        tabla.scan(ScanEnhancedRequest.builder().build()).stream()
                .flatMap(page -> page.items().stream())
                .forEach(item -> tabla.deleteItem(Key.builder()
                        .partitionValue(item.getId()).build()));
    }

    private Cliente reconstruirConSaldoDesencriptado(ClienteDynamoDb item) {
        BigDecimal saldoDesencriptado = new BigDecimal(
                encriptacion.desencriptar(item.getSaldoEncriptado()));
        Cliente base = item.toDomain();
        return new Cliente(
                base.getId(), base.getNombre(), saldoDesencriptado,
                base.getEmail(), base.getTelefono(), base.getDocumentoIdentidad(),
                base.getContrasenaHash(), base.getRol(),
                base.isCuentaBloqueada(), base.getIntentosFallidosLogin(),
                base.getFechaRegistro()
        );
    }
}
