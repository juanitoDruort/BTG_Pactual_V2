package btg_pactual_v1.btg_pactual_v2.domain.model;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;

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

    public BigDecimal cancelar(String nombreFondo) {
        if (this.estado != Estado.ACTIVO) {
            throw new ExcepcionDominio(
                "No se puede cancelar el " + nombreFondo + " ya que no esta activo dentro de su portafolio"
            );
        }
        this.estado = Estado.CANCELADO;
        return this.monto;
    }

    public String getId() { return id; }
    public String getClienteId() { return clienteId; }
    public String getFondoId() { return fondoId; }
    public BigDecimal getMonto() { return monto; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getFechaSuscripcion() { return fechaSuscripcion; }
}
