package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
        almacen.put("cliente-1", new Cliente("cliente-1", "Juan Rodriguez", new BigDecimal("500000")));
        almacen.put("cliente-2", new Cliente("cliente-2", "Maria Lopez",    new BigDecimal("1000000")));
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
}
