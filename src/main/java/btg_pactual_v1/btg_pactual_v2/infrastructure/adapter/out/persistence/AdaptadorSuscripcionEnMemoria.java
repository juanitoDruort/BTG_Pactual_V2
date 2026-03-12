package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdaptadorSuscripcionEnMemoria implements PuertoRepositorioSuscripcion {

    private final Map<String, Suscripcion> almacen = new ConcurrentHashMap<>();

    public void reiniciar() {
        almacen.clear();
    }

    @Override
    public Suscripcion guardar(Suscripcion suscripcion) {
        almacen.put(suscripcion.getId(), suscripcion);
        return suscripcion;
    }

    @Override
    public boolean existePorClienteIdYFondoId(String clienteId, String fondoId) {
        return almacen.values().stream().anyMatch(s ->
            s.getClienteId().equals(clienteId)
            && s.getFondoId().equals(fondoId)
            && s.getEstado() == Suscripcion.Estado.ACTIVO
        );
    }

    @Override
    public Optional<Suscripcion> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public List<Suscripcion> buscarPorClienteIdYEstado(String clienteId, Suscripcion.Estado estado) {
        return almacen.values().stream()
                .filter(s -> s.getClienteId().equals(clienteId) && s.getEstado() == estado)
                .toList();
    }
}
