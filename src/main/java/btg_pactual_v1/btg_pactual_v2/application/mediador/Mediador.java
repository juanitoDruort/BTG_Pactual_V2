package btg_pactual_v1.btg_pactual_v2.application.mediador;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mediador (equivalente a MediatR en .NET):
 * recibe cualquier solicitud y resuelve automáticamente el handler
 * registrado para su tipo. El emisor no conoce al receptor.
 */
@Component
public class Mediador {

    private final Map<Class<?>, Manejador<?, ?>> registro;

    public Mediador(List<Manejador<?, ?>> manejadores) {
        this.registro = manejadores.stream()
                .collect(Collectors.toMap(Manejador::tipoDeSolicitud, m -> m));
    }

    @SuppressWarnings("unchecked")
    public <T, R> R enviar(T solicitud) {
        Manejador<T, R> manejador = (Manejador<T, R>) registro.get(solicitud.getClass());
        if (manejador == null) {
            throw new IllegalArgumentException(
                "No hay manejador registrado para: " + solicitud.getClass().getSimpleName()
            );
        }
        return manejador.manejar(solicitud);
    }
}
