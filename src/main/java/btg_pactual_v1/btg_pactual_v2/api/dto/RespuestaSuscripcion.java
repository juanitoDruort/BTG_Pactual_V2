package btg_pactual_v1.btg_pactual_v2.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RespuestaSuscripcion(
        String suscripcionId,
        String clienteId,
        String fondoId,
        BigDecimal monto,
        String estado,
        LocalDateTime fechaSuscripcion
) {}
