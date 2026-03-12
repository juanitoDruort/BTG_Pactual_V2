package btg_pactual_v1.btg_pactual_v2.application.fondo.query;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioFondo;
import org.springframework.stereotype.Component;

/**
 * CQRS — Query: orquesta la consulta de un fondo.
 * El Mediador lo resuelve automáticamente al recibir una FondoConsulta.
 */
@Component("fondoConsultaManejador")
public class FondoManejador implements Manejador<FondoConsulta, FondoResultado> {

    private final IServicioFondo servicioFondo;

    public FondoManejador(IServicioFondo servicioFondo) {
        this.servicioFondo = servicioFondo;
    }

    @Override
    public FondoResultado manejar(FondoConsulta consulta) {
        return servicioFondo.obtener(consulta);
    }

    @Override
    public Class<FondoConsulta> tipoDeSolicitud() {
        return FondoConsulta.class;
    }
}
