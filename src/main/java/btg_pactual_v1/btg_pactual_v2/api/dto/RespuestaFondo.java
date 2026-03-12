package btg_pactual_v1.btg_pactual_v2.api.dto;

import java.math.BigDecimal;

public record RespuestaFondo(
        String id,
        String nombre,
        BigDecimal montoMinimo,
        String categoria
) {}
