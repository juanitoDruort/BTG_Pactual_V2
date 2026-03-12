package btg_pactual_v1.btg_pactual_v2.domain.service;

import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;

import java.math.BigDecimal;

public interface IServicioSuscripcion {

    Suscripcion suscribir(String clienteId, String fondoId, BigDecimal monto);
}
