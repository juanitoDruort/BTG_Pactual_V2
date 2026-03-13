package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb;

import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;

@DynamoDbBean
public class FondoDynamoDb {

    private String id;
    private String nombre;
    private BigDecimal montoMinimo;
    private String categoria;

    public FondoDynamoDb() {
    }

    @DynamoDbPartitionKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getMontoMinimo() { return montoMinimo; }
    public void setMontoMinimo(BigDecimal montoMinimo) { this.montoMinimo = montoMinimo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public static FondoDynamoDb fromDomain(Fondo fondo) {
        FondoDynamoDb item = new FondoDynamoDb();
        item.setId(fondo.getId());
        item.setNombre(fondo.getNombre());
        item.setMontoMinimo(fondo.getMontoMinimo());
        item.setCategoria(fondo.getCategoria());
        return item;
    }

    public Fondo toDomain() {
        return new Fondo(id, nombre, montoMinimo, categoria);
    }
}
