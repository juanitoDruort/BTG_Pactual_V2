package btg_pactual_v1.btg_pactual_v2.infrastructure.service;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionAccesoDenegado;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.model.Notificacion;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoNotificacion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.service.IServicioSuscripcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ServicioSuscripcion implements IServicioSuscripcion {

    private static final Logger log = LoggerFactory.getLogger(ServicioSuscripcion.class);

    private final PuertoRepositorioFondo repositorioFondo;
    private final PuertoRepositorioCliente repositorioCliente;
    private final PuertoRepositorioSuscripcion repositorioSuscripcion;
    private final PuertoNotificacion puertoNotificacion;

    public ServicioSuscripcion(PuertoRepositorioFondo repositorioFondo,
                               PuertoRepositorioCliente repositorioCliente,
                               PuertoRepositorioSuscripcion repositorioSuscripcion,
                               PuertoNotificacion puertoNotificacion) {
        this.repositorioFondo = repositorioFondo;
        this.repositorioCliente = repositorioCliente;
        this.repositorioSuscripcion = repositorioSuscripcion;
        this.puertoNotificacion = puertoNotificacion;
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

        Suscripcion suscripcion = repositorioSuscripcion.guardar(new Suscripcion(
                UUID.randomUUID().toString(),
                clienteId,
                fondoId,
                monto,
                Suscripcion.Estado.ACTIVO,
                LocalDateTime.now()
        ));

        try {
            puertoNotificacion.enviar(new Notificacion(
                    cliente.getEmail(),
                    Notificacion.Canal.EMAIL,
                    fondo.getNombre(),
                    monto,
                    suscripcion.getFechaSuscripcion()
            ));
        } catch (Exception e) {
            log.warn("Fallo al enviar notificación de suscripción para cliente {}: {}",
                    clienteId, e.getMessage());
        }

        return suscripcion;
    }

    @Override
    public Suscripcion cancelar(String suscripcionId, String clienteId) {
        Suscripcion suscripcion = repositorioSuscripcion.buscarPorId(suscripcionId)
                .orElseThrow(() -> new ExcepcionDominio("Suscripción no encontrada: " + suscripcionId));

        if (!suscripcion.getClienteId().equals(clienteId)) {
            throw new ExcepcionAccesoDenegado("No tiene permisos para cancelar esta suscripción");
        }

        Fondo fondo = repositorioFondo.buscarPorId(suscripcion.getFondoId())
                .orElseThrow(() -> new ExcepcionDominio("Fondo no encontrado: " + suscripcion.getFondoId()));

        BigDecimal montoDevuelto = suscripcion.cancelar(fondo.getNombre());

        Cliente cliente = repositorioCliente.buscarPorId(clienteId)
                .orElseThrow(() -> new ExcepcionDominio("Cliente no encontrado: " + clienteId));
        cliente.abonarSaldo(montoDevuelto);

        repositorioCliente.guardar(cliente);
        repositorioSuscripcion.guardar(suscripcion);

        return suscripcion;
    }
}
