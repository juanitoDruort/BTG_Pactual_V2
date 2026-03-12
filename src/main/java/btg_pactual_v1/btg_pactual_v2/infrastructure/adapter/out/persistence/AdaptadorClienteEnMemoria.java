package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Rol;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdaptadorClienteEnMemoria implements PuertoRepositorioCliente {

    private final Map<String, Cliente> almacen = new ConcurrentHashMap<>();

    public AdaptadorClienteEnMemoria() {
        cargarDatosIniciales();
    }

    public void reiniciar() {
        almacen.clear();
        cargarDatosIniciales();
    }

    private void cargarDatosIniciales() {
        almacen.put("cliente-1", new Cliente("cliente-1", "Juan Rodriguez", new BigDecimal("500000"),
                "juan.rodriguez@email.com", "3001234567", "CC123456789",
                "$2a$10$dummyHashParaDatosIniciales1", Rol.CLIENTE,
                false, 0, LocalDateTime.of(2026, 1, 1, 0, 0)));
        almacen.put("cliente-2", new Cliente("cliente-2", "Maria Lopez", new BigDecimal("1000000"),
                "maria.lopez@email.com", "3009876543", "CC987654321",
                "$2a$10$dummyHashParaDatosIniciales2", Rol.CLIENTE,
                false, 0, LocalDateTime.of(2026, 1, 1, 0, 0)));
    }

    @Override
    public Optional<Cliente> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        almacen.put(cliente.getId(), cliente);
        return cliente;
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        return almacen.values().stream()
                .filter(c -> c.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public boolean existePorEmail(String email) {
        return almacen.values().stream()
                .anyMatch(c -> c.getEmail().equals(email));
    }
}
