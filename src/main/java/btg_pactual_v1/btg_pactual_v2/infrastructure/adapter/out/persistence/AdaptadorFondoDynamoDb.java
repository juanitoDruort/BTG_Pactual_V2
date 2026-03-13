package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb.FondoDynamoDb;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Component
public class AdaptadorFondoDynamoDb implements PuertoRepositorioFondo {

    private final DynamoDbTable<FondoDynamoDb> tabla;

    public AdaptadorFondoDynamoDb(DynamoDbEnhancedClient enhancedClient) {
        this.tabla = enhancedClient.table("fondos", TableSchema.fromBean(FondoDynamoDb.class));
    }

    @Override
    public Optional<Fondo> buscarPorId(String id) {
        FondoDynamoDb item = tabla.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(item).map(FondoDynamoDb::toDomain);
    }
}
