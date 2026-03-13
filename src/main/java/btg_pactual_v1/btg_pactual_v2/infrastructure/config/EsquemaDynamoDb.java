package btg_pactual_v1.btg_pactual_v2.infrastructure.config;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public final class EsquemaDynamoDb {

    private EsquemaDynamoDb() {
    }

    public static void crearTablas(DynamoDbClient client) {
        crearTablaClientes(client);
        crearTablaFondos(client);
        crearTablaSuscripciones(client);
    }

    private static void crearTablaClientes(DynamoDbClient client) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName("clientes")
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id").keyType(KeyType.HASH).build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder()
                                    .attributeName("email").attributeType(ScalarAttributeType.S).build())
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("email-index")
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName("email").keyType(KeyType.HASH).build())
                            .projection(Projection.builder()
                                    .projectionType(ProjectionType.ALL).build())
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException ignored) {
        }
    }

    private static void crearTablaFondos(DynamoDbClient client) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName("fondos")
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id").keyType(KeyType.HASH).build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException ignored) {
        }
    }

    private static void crearTablaSuscripciones(DynamoDbClient client) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName("suscripciones")
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("clienteId").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder()
                                    .attributeName("id").keyType(KeyType.RANGE).build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("clienteId").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder()
                                    .attributeName("id").attributeType(ScalarAttributeType.S).build())
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("id-index")
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName("id").keyType(KeyType.HASH).build())
                            .projection(Projection.builder()
                                    .projectionType(ProjectionType.ALL).build())
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException ignored) {
        }
    }
}
