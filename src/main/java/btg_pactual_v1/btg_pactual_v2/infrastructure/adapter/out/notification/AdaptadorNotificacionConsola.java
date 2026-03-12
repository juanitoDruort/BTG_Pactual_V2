package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.notification;

import btg_pactual_v1.btg_pactual_v2.domain.model.Notificacion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoNotificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AdaptadorNotificacionConsola implements PuertoNotificacion {

    private static final Logger log = LoggerFactory.getLogger(AdaptadorNotificacionConsola.class);

    @Override
    public void enviar(Notificacion notificacion) {
        log.info("[NOTIFICACION-{}] Destinatario: {}, Fondo: {}, Monto: ${}, Fecha: {}",
                notificacion.canal(),
                notificacion.destinatario(),
                notificacion.nombreFondo(),
                notificacion.monto(),
                notificacion.fecha());
    }
}
