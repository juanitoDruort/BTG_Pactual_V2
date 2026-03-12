package btg_pactual_v1.btg_pactual_v2.domain.port.out;

import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;

import java.util.Optional;

public interface PuertoRepositorioCliente {

    Optional<Cliente> buscarPorId(String id);

    Cliente guardar(Cliente cliente);
}
