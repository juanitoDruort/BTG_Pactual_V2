package btg_pactual_v1.btg_pactual_v2;

import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorClienteDynamoDb;
import btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.AdaptadorSuscripcionDynamoDb;
import btg_pactual_v1.btg_pactual_v2.infrastructure.config.InicializadorTablasDynamoDb;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
abstract class E2ETestBase {

    static final GenericContainer<?> DYNAMODB_CONTAINER;

    static {
        DYNAMODB_CONTAINER = new GenericContainer<>(DockerImageName.parse("amazon/dynamodb-local:latest"))
                .withExposedPorts(8000)
                .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb");
        DYNAMODB_CONTAINER.start();
    }

    @DynamicPropertySource
    static void dynamoDbProperties(DynamicPropertyRegistry registry) {
        String endpoint = "http://" + DYNAMODB_CONTAINER.getHost()
                + ":" + DYNAMODB_CONTAINER.getMappedPort(8000);
        registry.add("dynamodb.endpoint", () -> endpoint);
        registry.add("dynamodb.region", () -> "us-east-1");
        registry.add("dynamodb.access-key", () -> "fakeMyKeyId");
        registry.add("dynamodb.secret-key", () -> "fakeSecretAccessKey");
    }

    @Autowired
    protected AdaptadorClienteDynamoDb adaptadorCliente;

    @Autowired
    protected AdaptadorSuscripcionDynamoDb adaptadorSuscripcion;

    @Autowired
    protected InicializadorTablasDynamoDb inicializador;

    @BeforeEach
    void reiniciarEstado() {
        adaptadorCliente.reiniciar();
        adaptadorSuscripcion.reiniciar();
        inicializador.insertarDatosSemilla();
    }
}
