package btg_pactual_v1.btg_pactual_v2.domain.port.out;

import btg_pactual_v1.btg_pactual_v2.domain.model.Notificacion;

public interface PuertoNotificacion {

    void enviar(Notificacion notificacion);
}
