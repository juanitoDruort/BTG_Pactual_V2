package btg_pactual_v1.btg_pactual_v2.domain.port.out;

import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;

import java.util.Optional;

public interface PuertoRepositorioFondo {

    Optional<Fondo> buscarPorId(String id);
}
