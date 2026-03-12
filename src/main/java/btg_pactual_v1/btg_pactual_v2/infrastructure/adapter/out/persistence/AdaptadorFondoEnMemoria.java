package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence;

import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdaptadorFondoEnMemoria implements PuertoRepositorioFondo {

    private final Map<String, Fondo> almacen = new ConcurrentHashMap<>();

    public AdaptadorFondoEnMemoria() {
        almacen.put("fondo-1", new Fondo("fondo-1", "FPV_BTG_PACTUAL_RECAUDADORA",  new BigDecimal("75000"),  "FPV"));
        almacen.put("fondo-2", new Fondo("fondo-2", "FPV_BTG_PACTUAL_ECOPETROL",    new BigDecimal("125000"), "FPV"));
        almacen.put("fondo-3", new Fondo("fondo-3", "DEUDAPRIVADA",                  new BigDecimal("50000"),  "FIC"));
        almacen.put("fondo-4", new Fondo("fondo-4", "FDO-ACCIONES",                  new BigDecimal("250000"), "FIC"));
        almacen.put("fondo-5", new Fondo("fondo-5", "FPV_BTG_PACTUAL_DINAMICA",     new BigDecimal("100000"), "FPV"));
    }

    @Override
    public Optional<Fondo> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }
}
