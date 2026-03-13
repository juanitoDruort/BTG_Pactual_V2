package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb;

import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DynamoDbBean
public class SuscripcionDynamoDb {

    private String clienteId;
    private String id;
    private String fondoId;
    private String nombreFondo;
    private BigDecimal monto;
    private String estado;
    private String fechaSuscripcion;

    public SuscripcionDynamoDb() {
    }

    @DynamoDbPartitionKey
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    @DynamoDbSortKey
    @DynamoDbSecondaryPartitionKey(indexNames = "id-index")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFondoId() { return fondoId; }
    public void setFondoId(String fondoId) { this.fondoId = fondoId; }

    public String getNombreFondo() { return nombreFondo; }
    public void setNombreFondo(String nombreFondo) { this.nombreFondo = nombreFondo; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaSuscripcion() { return fechaSuscripcion; }
    public void setFechaSuscripcion(String fechaSuscripcion) { this.fechaSuscripcion = fechaSuscripcion; }

    public static SuscripcionDynamoDb fromDomain(Suscripcion suscripcion, String nombreFondo) {
        SuscripcionDynamoDb item = new SuscripcionDynamoDb();
        item.setClienteId(suscripcion.getClienteId());
        item.setId(suscripcion.getId());
        item.setFondoId(suscripcion.getFondoId());
        item.setNombreFondo(nombreFondo);
        item.setMonto(suscripcion.getMonto());
        item.setEstado(suscripcion.getEstado().name());
        item.setFechaSuscripcion(suscripcion.getFechaSuscripcion() != null
                ? suscripcion.getFechaSuscripcion().toString() : null);
        return item;
    }

    public Suscripcion toDomain() {
        return new Suscripcion(
                id, clienteId, fondoId, monto,
                Suscripcion.Estado.valueOf(estado),
                fechaSuscripcion != null ? LocalDateTime.parse(fechaSuscripcion) : null
        );
    }
}
