package btg_pactual_v1.btg_pactual_v2.domain.service;

import btg_pactual_v1.btg_pactual_v2.application.fondo.query.FondoConsulta;
import btg_pactual_v1.btg_pactual_v2.application.fondo.query.FondoResultado;

public interface IServicioFondo {

    FondoResultado obtener(FondoConsulta consulta);
}
