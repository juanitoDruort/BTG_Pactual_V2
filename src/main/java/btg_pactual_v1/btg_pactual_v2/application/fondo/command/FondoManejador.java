package btg_pactual_v1.btg_pactual_v2.application.fondo.command;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioSuscripcion;
import org.springframework.stereotype.Component;

/**
 * CQRS — Command: orquesta la suscripción a un fondo.
 * El Mediador lo resuelve automáticamente al recibir un FondoComando.
 */
@Component("fondoComandoManejador")
public class FondoManejador implements Manejador<FondoComando, FondoResultado> {

    private final IServicioSuscripcion servicioSuscripcion;

    public FondoManejador(IServicioSuscripcion servicioSuscripcion) {
        this.servicioSuscripcion = servicioSuscripcion;
    }

    @Override
    public FondoResultado manejar(FondoComando comando) {
        return servicioSuscripcion.suscribir(comando);
    }

    @Override
    public Class<FondoComando> tipoDeSolicitud() {
        return FondoComando.class;
    }
}
