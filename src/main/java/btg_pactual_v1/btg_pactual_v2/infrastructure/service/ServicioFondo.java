package btg_pactual_v1.btg_pactual_v2.infrastructure.service;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioFondo;
import org.springframework.stereotype.Service;

@Service
public class ServicioFondo implements IServicioFondo {

    private final PuertoRepositorioFondo repositorioFondo;

    public ServicioFondo(PuertoRepositorioFondo repositorioFondo) {
        this.repositorioFondo = repositorioFondo;
    }

    @Override
    public Fondo obtener(String fondoId) {
        return repositorioFondo.buscarPorId(fondoId)
                .orElseThrow(() -> new ExcepcionDominio("Fondo no encontrado: " + fondoId));
    }
}
