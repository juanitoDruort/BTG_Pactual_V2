package btg_pactual_v1.btg_pactual_v2.domain.model;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;

import java.math.BigDecimal;

public class Cliente {

    private final String id;
    private final String nombre;
    private BigDecimal saldo;

    public Cliente(String id, String nombre, BigDecimal saldo) {
        this.id = id;
        this.nombre = nombre;
        this.saldo = saldo;
    }

    public void descontarSaldo(BigDecimal monto, String nombreFondo) {
        if (this.saldo.compareTo(monto) < 0) {
            throw new ExcepcionDominio(
                "No tiene saldo disponible para vincularse al fondo " + nombreFondo + ". Saldo actual: " + this.saldo
            );
        }
        this.saldo = this.saldo.subtract(monto);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getSaldo() { return saldo; }
}
