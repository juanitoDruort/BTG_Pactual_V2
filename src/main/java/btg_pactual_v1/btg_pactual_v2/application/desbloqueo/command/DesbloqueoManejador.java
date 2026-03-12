package btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.springframework.stereotype.Component;

@Component
public class DesbloqueoManejador implements Manejador<DesbloqueoComando, DesbloqueoResultado> {

    private final PuertoRepositorioCliente repositorioCliente;

    public DesbloqueoManejador(PuertoRepositorioCliente repositorioCliente) {
        this.repositorioCliente = repositorioCliente;
    }

    @Override
    public DesbloqueoResultado manejar(DesbloqueoComando comando) {
        Cliente cliente = repositorioCliente.buscarPorId(comando.clienteId())
                .orElseThrow(() -> new ExcepcionDominio("Cliente no encontrado: " + comando.clienteId()));

        cliente.desbloquearCuenta();
        repositorioCliente.guardar(cliente);

        return new DesbloqueoResultado(
                cliente.getId(),
                cliente.getEmail(),
                "Cuenta desbloqueada exitosamente"
        );
    }

    @Override
    public Class<DesbloqueoComando> tipoDeSolicitud() {
        return DesbloqueoComando.class;
    }
}
