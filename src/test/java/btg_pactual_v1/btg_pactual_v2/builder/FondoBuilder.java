package btg_pactual_v1.btg_pactual_v2.builder;

import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;

import java.math.BigDecimal;

public class FondoBuilder {

    private String id = "fondo-test";
    private String nombre = "FONDO_TEST";
    private BigDecimal montoMinimo = new BigDecimal("75000");
    private String categoria = "FPV";

    public FondoBuilder conId(String id) {
        this.id = id;
        return this;
    }

    public FondoBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public FondoBuilder conMontoMinimo(BigDecimal montoMinimo) {
        this.montoMinimo = montoMinimo;
        return this;
    }

    public FondoBuilder conCategoria(String categoria) {
        this.categoria = categoria;
        return this;
    }

    public Fondo build() {
        return new Fondo(id, nombre, montoMinimo, categoria);
    }
}
