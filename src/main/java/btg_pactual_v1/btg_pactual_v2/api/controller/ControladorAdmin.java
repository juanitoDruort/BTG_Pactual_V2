package btg_pactual_v1.btg_pactual_v2.api.controller;

import btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command.DesbloqueoComando;
import btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command.DesbloqueoResultado;
import btg_pactual_v1.btg_pactual_v2.application.mediador.Mediador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración", description = "Operaciones exclusivas de administradores")
public class ControladorAdmin {

    private final Mediador mediador;

    public ControladorAdmin(Mediador mediador) {
        this.mediador = mediador;
    }

    @PutMapping("/usuarios/{userId}/desbloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desbloqueo de cuenta por administrador (Command)")
    public ResponseEntity<DesbloqueoResultado> desbloquear(@PathVariable String userId) {
        DesbloqueoResultado resultado = mediador.enviar(new DesbloqueoComando(userId));
        return ResponseEntity.ok(resultado);
    }
}
