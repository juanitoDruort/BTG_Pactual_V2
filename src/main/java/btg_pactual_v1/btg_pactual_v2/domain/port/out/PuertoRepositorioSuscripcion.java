package btg_pactual_v1.btg_pactual_v2.domain.port.out;

import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;

public interface PuertoRepositorioSuscripcion {

    Suscripcion guardar(Suscripcion suscripcion);

    boolean existePorClienteIdYFondoId(String clienteId, String fondoId);
}
