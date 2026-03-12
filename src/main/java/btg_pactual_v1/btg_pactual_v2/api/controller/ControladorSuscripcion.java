package btg_pactual_v1.btg_pactual_v2.api.controller;

import btg_pactual_v1.btg_pactual_v2.api.dto.RespuestaSuscripcion;
import btg_pactual_v1.btg_pactual_v2.application.cancelacion.command.CancelacionComando;
import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoResultado;
import btg_pactual_v1.btg_pactual_v2.application.mediador.Mediador;
import btg_pactual_v1.btg_pactual_v2.application.suscripcion.query.SuscripcionVigenteResultado;
import btg_pactual_v1.btg_pactual_v2.application.suscripcion.query.SuscripcionesVigentesConsulta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suscripciones")
@Tag(name = "Suscripciones", description = "Operaciones sobre suscripciones a fondos")
public class ControladorSuscripcion {

    private final Mediador mediador;

    public ControladorSuscripcion(Mediador mediador) {
        this.mediador = mediador;
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar suscripción a un fondo (Command)")
    public ResponseEntity<RespuestaSuscripcion> cancelar(@PathVariable String id,
                                                         Authentication auth) {
        String clienteId = auth.getName();
        FondoResultado resultado = mediador.enviar(new CancelacionComando(id, clienteId));
        return ResponseEntity.ok(new RespuestaSuscripcion(
                resultado.suscripcionId(),
                resultado.clienteId(),
                resultado.fondoId(),
                resultado.monto(),
                resultado.estado(),
                resultado.fechaSuscripcion()
        ));
    }

    @GetMapping("/vigentes")
    @Operation(summary = "Consultar suscripciones vigentes del cliente (Query)")
    public ResponseEntity<List<SuscripcionVigenteResultado>> vigentes(Authentication auth) {
        String clienteId = auth.getName();
        List<SuscripcionVigenteResultado> resultado = mediador.enviar(
                new SuscripcionesVigentesConsulta(clienteId));
        return ResponseEntity.ok(resultado);
    }
}
