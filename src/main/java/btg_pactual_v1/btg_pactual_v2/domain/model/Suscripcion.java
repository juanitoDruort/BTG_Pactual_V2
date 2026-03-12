package btg_pactual_v1.btg_pactual_v2.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Suscripcion {

    public enum Estado { ACTIVO, CANCELADO }

    private final String id;
    private final String clienteId;
    private final String fondoId;
    private final BigDecimal monto;
    private Estado estado;
    private final LocalDateTime fechaSuscripcion;

    public Suscripcion(String id, String clienteId, String fondoId, BigDecimal monto,
                       Estado estado, LocalDateTime fechaSuscripcion) {
        this.id = id;
        this.clienteId = clienteId;
        this.fondoId = fondoId;
        this.monto = monto;
        this.estado = estado;
        this.fechaSuscripcion = fechaSuscripcion;
    }

    public String getId() { return id; }
    public String getClienteId() { return clienteId; }
    public String getFondoId() { return fondoId; }
    public BigDecimal getMonto() { return monto; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getFechaSuscripcion() { return fechaSuscripcion; }
}
