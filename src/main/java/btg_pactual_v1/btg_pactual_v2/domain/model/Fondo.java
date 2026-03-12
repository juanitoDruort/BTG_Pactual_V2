package btg_pactual_v1.btg_pactual_v2.domain.model;

import java.math.BigDecimal;

public class Fondo {

    private final String id;
    private final String nombre;
    private final BigDecimal montoMinimo;
    private final String categoria;

    public Fondo(String id, String nombre, BigDecimal montoMinimo, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.montoMinimo = montoMinimo;
        this.categoria = categoria;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getMontoMinimo() { return montoMinimo; }
    public String getCategoria() { return categoria; }
}
