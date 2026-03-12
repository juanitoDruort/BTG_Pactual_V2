package btg_pactual_v1.btg_pactual_v2.application.fondo.command;

import java.math.BigDecimal;

public record FondoComando(String clienteId, String fondoId, BigDecimal monto) {}
