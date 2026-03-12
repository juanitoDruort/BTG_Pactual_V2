package btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command;

public record DesbloqueoResultado(
        String clienteId,
        String email,
        String mensaje
) {}
