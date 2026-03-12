package btg_pactual_v1.btg_pactual_v2.application.suscripcion.query;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;

import org.springframework.stereotype.Component;

import java.util.List;

@Component("suscripcionesVigentesConsultaManejador")
public class SuscripcionesVigentesManejador
        implements Manejador<SuscripcionesVigentesConsulta, List<SuscripcionVigenteResultado>> {

    private final PuertoRepositorioSuscripcion repositorioSuscripcion;
    private final PuertoRepositorioFondo repositorioFondo;

    public SuscripcionesVigentesManejador(PuertoRepositorioSuscripcion repositorioSuscripcion,
                                          PuertoRepositorioFondo repositorioFondo) {
        this.repositorioSuscripcion = repositorioSuscripcion;
        this.repositorioFondo = repositorioFondo;
    }

    @Override
    public List<SuscripcionVigenteResultado> manejar(SuscripcionesVigentesConsulta consulta) {
        List<Suscripcion> vigentes = repositorioSuscripcion
                .buscarPorClienteIdYEstado(consulta.clienteId(), Suscripcion.Estado.ACTIVO);

        return vigentes.stream()
                .map(s -> {
                    String nombreFondo = repositorioFondo.buscarPorId(s.getFondoId())
                            .map(Fondo::getNombre)
                            .orElse(s.getFondoId());
                    return new SuscripcionVigenteResultado(
                            s.getId(),
                            s.getFondoId(),
                            nombreFondo,
                            s.getMonto(),
                            s.getEstado().name(),
                            s.getFechaSuscripcion());
                })
                .toList();
    }

    @Override
    public Class<SuscripcionesVigentesConsulta> tipoDeSolicitud() {
        return SuscripcionesVigentesConsulta.class;
    }
}
