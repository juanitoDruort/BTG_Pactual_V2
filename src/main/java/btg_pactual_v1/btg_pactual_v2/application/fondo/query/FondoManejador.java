package btg_pactual_v1.btg_pactual_v2.application.fondo.query;

import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioFondo;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;

import org.springframework.stereotype.Component;

@Component("fondoConsultaManejador")
public class FondoManejador implements Manejador<FondoConsulta, FondoResultado> {

    private final IServicioFondo servicioFondo;

    public FondoManejador(IServicioFondo servicioFondo) {
        this.servicioFondo = servicioFondo;
    }

    @Override
    public FondoResultado manejar(FondoConsulta consulta) {
        Fondo fondo = servicioFondo.obtener(consulta.fondoId());

        return new FondoResultado(
                fondo.getId(),
                fondo.getNombre(),
                fondo.getMontoMinimo(),
                fondo.getCategoria());
    }

    @Override
    public Class<FondoConsulta> tipoDeSolicitud() {
        return FondoConsulta.class;
    }
}
