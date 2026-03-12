package btg_pactual_v1.btg_pactual_v2.application.cancelacion.command;

import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoResultado;
import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioSuscripcion;

import org.springframework.stereotype.Component;

@Component("cancelacionComandoManejador")
public class CancelacionManejador implements Manejador<CancelacionComando, FondoResultado> {

    private final IServicioSuscripcion servicioSuscripcion;

    public CancelacionManejador(IServicioSuscripcion servicioSuscripcion) {
        this.servicioSuscripcion = servicioSuscripcion;
    }

    @Override
    public FondoResultado manejar(CancelacionComando comando) {
        Suscripcion suscripcion = servicioSuscripcion.cancelar(
                comando.suscripcionId(), comando.clienteId());

        return new FondoResultado(
                suscripcion.getId(),
                suscripcion.getClienteId(),
                suscripcion.getFondoId(),
                suscripcion.getMonto(),
                suscripcion.getEstado().name(),
                suscripcion.getFechaSuscripcion());
    }

    @Override
    public Class<CancelacionComando> tipoDeSolicitud() {
        return CancelacionComando.class;
    }
}
