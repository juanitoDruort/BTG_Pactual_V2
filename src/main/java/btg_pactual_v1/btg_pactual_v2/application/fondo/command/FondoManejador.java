package btg_pactual_v1.btg_pactual_v2.application.fondo.command;

import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioSuscripcion;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;

import org.springframework.stereotype.Component;

@Component("fondoComandoManejador")
public class FondoManejador implements Manejador<FondoComando, FondoResultado> {

    private final IServicioSuscripcion servicioSuscripcion;

    public FondoManejador(IServicioSuscripcion servicioSuscripcion) {
        this.servicioSuscripcion = servicioSuscripcion;
    }

    @Override
    public FondoResultado manejar(FondoComando comando) {
        Suscripcion suscripcion = servicioSuscripcion.suscribir(
                comando.clienteId(), comando.fondoId(), comando.monto());

        return new FondoResultado(
                suscripcion.getId(),
                suscripcion.getClienteId(),
                suscripcion.getFondoId(),
                suscripcion.getMonto(),
                suscripcion.getEstado().name(),
                suscripcion.getFechaSuscripcion());
    }

    @Override
    public Class<FondoComando> tipoDeSolicitud() {
        return FondoComando.class;
    }
}
