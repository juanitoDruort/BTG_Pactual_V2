package btg_pactual_v1.btg_pactual_v2.application.fondo.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FondoComando(
        @NotBlank(message = "El clienteId es obligatorio")
        String clienteId,

        @NotBlank(message = "El fondoId es obligatorio")
        String fondoId,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal monto
) {}
