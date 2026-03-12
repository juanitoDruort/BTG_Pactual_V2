package btg_pactual_v1.btg_pactual_v2.api.controller;

import btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command.DesbloqueoComando;
import btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command.DesbloqueoResultado;
import btg_pactual_v1.btg_pactual_v2.application.login.command.LoginComando;
import btg_pactual_v1.btg_pactual_v2.application.login.command.LoginResultado;
import btg_pactual_v1.btg_pactual_v2.application.mediador.Mediador;
import btg_pactual_v1.btg_pactual_v2.application.registro.command.RegistroComando;
import btg_pactual_v1.btg_pactual_v2.application.registro.command.RegistroResultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro y autenticación de usuarios")
public class ControladorAutenticacion {

    private final Mediador mediador;

    public ControladorAutenticacion(Mediador mediador) {
        this.mediador = mediador;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registro de nuevo cliente (Command)")
    public ResponseEntity<RegistroResultado> registrar(@RequestBody @Valid RegistroComando comando) {
        RegistroResultado resultado = mediador.enviar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping("/login")
    @Operation(summary = "Login de usuario (Command)")
    public ResponseEntity<LoginResultado> login(@RequestBody @Valid LoginComando comando) {
        LoginResultado resultado = mediador.enviar(comando);
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/desbloqueo/{clienteId}")
    @Operation(summary = "Desbloqueo de cuenta por administrador (Command)")
    public ResponseEntity<DesbloqueoResultado> desbloquear(@PathVariable String clienteId) {
        DesbloqueoResultado resultado = mediador.enviar(new DesbloqueoComando(clienteId));
        return ResponseEntity.ok(resultado);
    }
}
