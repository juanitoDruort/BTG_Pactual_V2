package btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command;

import jakarta.validation.constraints.NotBlank;

public record DesbloqueoComando(
        @NotBlank(message = "El clienteId es obligatorio")
        String clienteId
) {}
