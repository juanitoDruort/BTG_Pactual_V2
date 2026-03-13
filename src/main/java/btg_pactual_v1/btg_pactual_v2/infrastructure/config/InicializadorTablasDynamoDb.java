package btg_pactual_v1.btg_pactual_v2.infrastructure.config;

import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoEncriptacion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoHashContrasena;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

@Component
public class InicializadorTablasDynamoDb {

    private final DynamoDbClient dynamoDbClient;
    private final PuertoHashContrasena hashContrasena;
    private final PuertoEncriptacion encriptacion;

    public InicializadorTablasDynamoDb(DynamoDbClient dynamoDbClient, PuertoHashContrasena hashContrasena,
                                       PuertoEncriptacion encriptacion) {
        this.dynamoDbClient = dynamoDbClient;
        this.hashContrasena = hashContrasena;
        this.encriptacion = encriptacion;
    }

    @PostConstruct
    public void inicializar() {
        EsquemaDynamoDb.crearTablas(dynamoDbClient);
        insertarDatosSemilla();
    }

    public void insertarDatosSemilla() {
        String hash1 = hashContrasena.hash("Pass1!");
        String hash2 = hashContrasena.hash("Pass2!");

        insertarCliente("cliente-1", "Juan Rodriguez", "500000",
                "juan.rodriguez@email.com", "3001234567", "CC123456789",
                hash1, "2026-01-01T00:00:00");
        insertarCliente("cliente-2", "Maria Lopez", "1000000",
                "maria.lopez@email.com", "3009876543", "CC987654321",
                hash2, "2026-01-01T00:00:00");

        insertarFondo("fondo-1", "FPV_BTG_PACTUAL_RECAUDADORA", "75000", "FPV");
        insertarFondo("fondo-2", "FPV_BTG_PACTUAL_ECOPETROL", "125000", "FPV");
        insertarFondo("fondo-3", "DEUDAPRIVADA", "50000", "FIC");
        insertarFondo("fondo-4", "FDO-ACCIONES", "250000", "FIC");
        insertarFondo("fondo-5", "FPV_BTG_PACTUAL_DINAMICA", "100000", "FPV");
    }

    private void insertarCliente(String id, String nombre, String saldo, String email,
                                  String telefono, String documentoIdentidad,
                                  String contrasenaHash, String fechaRegistro) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("nombre", AttributeValue.fromS(nombre));
        item.put("saldoEncriptado", AttributeValue.fromS(encriptacion.encriptar(saldo)));
        item.put("email", AttributeValue.fromS(email));
        item.put("telefono", AttributeValue.fromS(telefono));
        item.put("documentoIdentidad", AttributeValue.fromS(documentoIdentidad));
        item.put("contrasenaHash", AttributeValue.fromS(contrasenaHash));
        item.put("rol", AttributeValue.fromS("CLIENTE"));
        item.put("cuentaBloqueada", AttributeValue.fromBool(false));
        item.put("intentosFallidosLogin", AttributeValue.fromN("0"));
        item.put("fechaRegistro", AttributeValue.fromS(fechaRegistro));
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName("clientes").item(item).build());
    }

    private void insertarFondo(String id, String nombre, String montoMinimo, String categoria) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName("fondos")
                .item(Map.of(
                        "id", AttributeValue.fromS(id),
                        "nombre", AttributeValue.fromS(nombre),
                        "montoMinimo", AttributeValue.fromN(montoMinimo),
                        "categoria", AttributeValue.fromS(categoria)))
                .build());
    }
}
