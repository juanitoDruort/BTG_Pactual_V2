package btg_pactual_v1.btg_pactual_v2.infrastructure.service;

import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoComando;
import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoResultado;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioSuscripcion;
import org.springframework.stereotype.Service;

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
    public FondoResultado suscribir(FondoComando comando) {
        Fondo fondo = repositorioFondo.buscarPorId(comando.fondoId())
                .orElseThrow(() -> new ExcepcionDominio("Fondo no encontrado: " + comando.fondoId()));

        Cliente cliente = repositorioCliente.buscarPorId(comando.clienteId())
                .orElseThrow(() -> new ExcepcionDominio("Cliente no encontrado: " + comando.clienteId()));

        if (comando.monto().compareTo(fondo.getMontoMinimo()) < 0) {
            throw new ExcepcionDominio(
                "El monto mínimo para vincularse al fondo " + fondo.getNombre()
                + " es $" + fondo.getMontoMinimo()
            );
        }

        if (repositorioSuscripcion.existePorClienteIdYFondoId(comando.clienteId(), comando.fondoId())) {
            throw new ExcepcionDominio("El cliente ya está vinculado al fondo " + fondo.getNombre());
        }

        cliente.descontarSaldo(comando.monto());
        repositorioCliente.guardar(cliente);

        Suscripcion suscripcion = repositorioSuscripcion.guardar(new Suscripcion(
                UUID.randomUUID().toString(),
                comando.clienteId(),
                comando.fondoId(),
                comando.monto(),
                Suscripcion.Estado.ACTIVO,
                LocalDateTime.now()
        ));

        return new FondoResultado(
                suscripcion.getId(),
                suscripcion.getClienteId(),
                suscripcion.getFondoId(),
                suscripcion.getMonto(),
                suscripcion.getEstado().name(),
                suscripcion.getFechaSuscripcion()
        );
    }
}
