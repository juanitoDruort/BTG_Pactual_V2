package btg_pactual_v1.btg_pactual_v2.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Notificacion(
        String destinatario,
        Canal canal,
        String nombreFondo,
        BigDecimal monto,
        LocalDateTime fecha
) {
    public enum Canal { EMAIL, SMS }
}
