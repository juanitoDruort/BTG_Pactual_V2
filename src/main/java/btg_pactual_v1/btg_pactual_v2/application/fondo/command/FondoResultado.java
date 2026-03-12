package btg_pactual_v1.btg_pactual_v2.application.fondo.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FondoResultado(
        String suscripcionId,
        String clienteId,
        String fondoId,
        BigDecimal monto,
        String estado,
        LocalDateTime fechaSuscripcion
) {}
