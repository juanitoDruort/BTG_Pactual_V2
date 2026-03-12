package btg_pactual_v1.btg_pactual_v2.application.fondo.query;

import java.math.BigDecimal;

public record FondoResultado(
        String id,
        String nombre,
        BigDecimal montoMinimo,
        String categoria
) {}
