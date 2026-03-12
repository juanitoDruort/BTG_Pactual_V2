package btg_pactual_v1.btg_pactual_v2.api.controller;

import btg_pactual_v1.btg_pactual_v2.api.dto.RespuestaFondo;
import btg_pactual_v1.btg_pactual_v2.api.dto.RespuestaSuscripcion;
import btg_pactual_v1.btg_pactual_v2.api.dto.SolicitudSuscripcion;
import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoComando;
import btg_pactual_v1.btg_pactual_v2.application.fondo.query.FondoConsulta;
import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoResultado;
import btg_pactual_v1.btg_pactual_v2.application.mediador.Mediador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fondos")
@Tag(name = "Fondos", description = "Operaciones sobre fondos BTG Pactual")
public class ControladorFondo {

    private final Mediador mediador;

    public ControladorFondo(Mediador mediador) {
        this.mediador = mediador;
    }

    @PostMapping("/suscribir")
    @Operation(summary = "Suscribir cliente a un fondo (Command)")
    public ResponseEntity<RespuestaSuscripcion> suscribir(@RequestBody @Valid SolicitudSuscripcion solicitud) {
        FondoResultado resultado = mediador.enviar(
                new FondoComando(solicitud.clienteId(), solicitud.fondoId(), solicitud.monto())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new RespuestaSuscripcion(
                resultado.suscripcionId(),
                resultado.clienteId(),
                resultado.fondoId(),
                resultado.monto(),
                resultado.estado(),
                resultado.fechaSuscripcion()
        ));
    }

    @GetMapping("/{fondoId}")
    @Operation(summary = "Consultar fondo por ID (Query)")
    public ResponseEntity<RespuestaFondo> obtenerFondo(@PathVariable String fondoId) {
        btg_pactual_v1.btg_pactual_v2.application.fondo.query.FondoResultado resultado =
                mediador.enviar(new FondoConsulta(fondoId));
        return ResponseEntity.ok(new RespuestaFondo(
                resultado.id(),
                resultado.nombre(),
                resultado.montoMinimo(),
                resultado.categoria()
        ));
    }
}
