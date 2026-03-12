package btg_pactual_v1.btg_pactual_v2.application.mediador;

/**
 * Contrato genérico que todo handler debe implementar.
 * T = tipo de la solicitud (comando o consulta)
 * R = tipo del resultado
 */
public interface Manejador<T, R> {

    R manejar(T solicitud);

    Class<T> tipoDeSolicitud();
}
