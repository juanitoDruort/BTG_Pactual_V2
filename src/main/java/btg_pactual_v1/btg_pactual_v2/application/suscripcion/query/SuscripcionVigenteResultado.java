package btg_pactual_v1.btg_pactual_v2.application.suscripcion.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SuscripcionVigenteResultado(
        String suscripcionId,
        String fondoId,
        String nombreFondo,
        BigDecimal monto,
        String estado,
        LocalDateTime fechaSuscripcion
) {}
