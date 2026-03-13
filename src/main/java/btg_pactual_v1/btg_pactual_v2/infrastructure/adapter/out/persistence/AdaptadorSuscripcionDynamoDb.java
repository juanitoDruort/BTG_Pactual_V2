package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.FondoDynamoDb;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.SuscripcionDynamoDb;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorSuscripcionDynamoDb implements PuertoRepositorioSuscripcion {

    private final DynamoDbTable<SuscripcionDynamoDb> tabla;
    private final DynamoDbIndex<SuscripcionDynamoDb> idIndex;
    private final DynamoDbTable<FondoDynamoDb> tablaFondos;

    public AdaptadorSuscripcionDynamoDb(DynamoDbEnhancedClient enhancedClient) {
        this.tabla = enhancedClient.table("suscripciones", TableSchema.fromBean(SuscripcionDynamoDb.class));
        this.idIndex = tabla.index("id-index");
        this.tablaFondos = enhancedClient.table("fondos", TableSchema.fromBean(FondoDynamoDb.class));
    }

    @Override
    public Suscripcion guardar(Suscripcion suscripcion) {
        String nombreFondo = resolverNombreFondo(suscripcion.getFondoId());
        SuscripcionDynamoDb item = SuscripcionDynamoDb.fromDomain(suscripcion, nombreFondo);
        tabla.putItem(item);
        return suscripcion;
    }

    @Override
    public boolean existePorClienteIdYFondoId(String clienteId, String fondoId) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(clienteId).build()))
                .build();

        return tabla.query(request).stream()
                .flatMap(page -> page.items().stream())
                .anyMatch(item -> item.getFondoId().equals(fondoId)
                        && Suscripcion.Estado.ACTIVO.name().equals(item.getEstado()));
    }

    @Override
    public Optional<Suscripcion> buscarPorId(String id) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(id).build()))
                .limit(1)
                .build();

        return idIndex.query(request).stream()
                .flatMap(page -> page.items().stream())
                .findFirst()
                .map(SuscripcionDynamoDb::toDomain);
    }

    @Override
    public List<Suscripcion> buscarPorClienteIdYEstado(String clienteId, Suscripcion.Estado estado) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(clienteId).build()))
                .build();

        return tabla.query(request).stream()
                .flatMap(page -> page.items().stream())
                .filter(item -> estado.name().equals(item.getEstado()))
                .map(SuscripcionDynamoDb::toDomain)
                .toList();
    }

    public void reiniciar() {
        tabla.scan(ScanEnhancedRequest.builder().build()).stream()
                .flatMap(page -> page.items().stream())
                .forEach(item -> tabla.deleteItem(Key.builder()
                        .partitionValue(item.getClienteId())
                        .sortValue(item.getId())
                        .build()));
    }

    private String resolverNombreFondo(String fondoId) {
        FondoDynamoDb fondo = tablaFondos.getItem(Key.builder().partitionValue(fondoId).build());
        return fondo != null ? fondo.getNombre() : fondoId;
    }
}
