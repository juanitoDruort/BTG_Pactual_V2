package btg_pactual_v1.btg_pactual_v2.builder;

import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SuscripcionBuilder {

    private String id = "suscripcion-test";
    private String clienteId = "cliente-test";
    private String fondoId = "fondo-test";
    private BigDecimal monto = new BigDecimal("75000");
    private Suscripcion.Estado estado = Suscripcion.Estado.ACTIVO;
    private LocalDateTime fechaSuscripcion = LocalDateTime.now();

    public SuscripcionBuilder conId(String id) {
        this.id = id;
        return this;
    }

    public SuscripcionBuilder conClienteId(String clienteId) {
        this.clienteId = clienteId;
        return this;
    }

    public SuscripcionBuilder conFondoId(String fondoId) {
        this.fondoId = fondoId;
        return this;
    }

    public SuscripcionBuilder conMonto(BigDecimal monto) {
        this.monto = monto;
        return this;
    }

    public SuscripcionBuilder conEstado(Suscripcion.Estado estado) {
        this.estado = estado;
        return this;
    }

    public SuscripcionBuilder conFechaSuscripcion(LocalDateTime fechaSuscripcion) {
        this.fechaSuscripcion = fechaSuscripcion;
        return this;
    }

    public Suscripcion build() {
        return new Suscripcion(id, clienteId, fondoId, monto, estado, fechaSuscripcion);
    }
}
