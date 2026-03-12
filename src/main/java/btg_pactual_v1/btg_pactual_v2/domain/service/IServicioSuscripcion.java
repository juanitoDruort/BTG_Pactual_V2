package btg_pactual_v1.btg_pactual_v2.domain.service;

import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoComando;
import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoResultado;

public interface IServicioSuscripcion {

    FondoResultado suscribir(FondoComando comando);
}
