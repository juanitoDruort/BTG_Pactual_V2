package btg_pactual_v1.btg_pactual_v2.infrastructure.service;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioSuscripcion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ServicioSuscripcion implements IServicioSuscripcion {

    private final PuertoRepositorioFondo repositorioFondo;
    private final PuertoRepositorioCliente repositorioCliente;
    private final PuertoRepositorioSuscripcion repositorioSuscripcion;

    public ServicioSuscripcion(PuertoRepositorioFondo repositorioFondo,
                               PuertoRepositorioCliente repositorioCliente,
                               PuertoRepositorioSuscripcion repositorioSuscripcion) {
        this.repositorioFondo = repositorioFondo;
        this.repositorioCliente = repositorioCliente;
        this.repositorioSuscripcion = repositorioSuscripcion;
    }

    @Override
    public Suscripcion suscribir(String clienteId, String fondoId, BigDecimal monto) {
        Fondo fondo = repositorioFondo.buscarPorId(fondoId)
                .orElseThrow(() -> new ExcepcionDominio("Fondo no encontrado: " + fondoId));

        Cliente cliente = repositorioCliente.buscarPorId(clienteId)
                .orElseThrow(() -> new ExcepcionDominio("Cliente no encontrado: " + clienteId));

        if (monto.compareTo(fondo.getMontoMinimo()) < 0) {
            throw new ExcepcionDominio(
                "El monto mínimo para vincularse al fondo " + fondo.getNombre()
                + " es $" + fondo.getMontoMinimo()
            );
        }

        if (repositorioSuscripcion.existePorClienteIdYFondoId(clienteId, fondoId)) {
            throw new ExcepcionDominio("El cliente ya está vinculado al fondo " + fondo.getNombre());
        }

        cliente.descontarSaldo(monto, fondo.getNombre());
        repositorioCliente.guardar(cliente);

        return repositorioSuscripcion.guardar(new Suscripcion(
                UUID.randomUUID().toString(),
                clienteId,
                fondoId,
                monto,
                Suscripcion.Estado.ACTIVO,
                LocalDateTime.now()
        ));
    }
}
