package btg_pactual_v1.btg_pactual_v2.application.login.command;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionCredencialesInvalidas;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionCuentaBloqueada;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoGenerarToken;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoHashContrasena;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginManejador implements Manejador<LoginComando, LoginResultado> {

    private final PuertoRepositorioCliente repositorioCliente;
    private final PuertoHashContrasena hashContrasena;
    private final PuertoGenerarToken generarToken;
    private final int maxIntentosFallidos;

    public LoginManejador(PuertoRepositorioCliente repositorioCliente,
                          PuertoHashContrasena hashContrasena,
                          PuertoGenerarToken generarToken,
                          @Value("${jwt.max-failed-attempts}") int maxIntentosFallidos) {
        this.repositorioCliente = repositorioCliente;
        this.hashContrasena = hashContrasena;
        this.generarToken = generarToken;
        this.maxIntentosFallidos = maxIntentosFallidos;
    }

    @Override
    public LoginResultado manejar(LoginComando comando) {
        Cliente cliente = repositorioCliente.buscarPorEmail(comando.email())
                .orElseThrow(() -> new ExcepcionCredencialesInvalidas("Credenciales inválidas"));

        if (cliente.estaBloqueada()) {
            throw new ExcepcionCuentaBloqueada("La cuenta está bloqueada por múltiples intentos fallidos. Contacte al administrador");
        }

        if (!hashContrasena.verificar(comando.contrasena(), cliente.getContrasenaHash())) {
            cliente.incrementarIntentosFallidos();
            if (cliente.getIntentosFallidosLogin() >= maxIntentosFallidos) {
                cliente.bloquearCuenta();
            }
            repositorioCliente.guardar(cliente);
            throw new ExcepcionCredencialesInvalidas("Credenciales inválidas");
        }

        cliente.resetearIntentosFallidos();
        repositorioCliente.guardar(cliente);

        String token = generarToken.generarToken(cliente.getId(), cliente.getRol().name());
        return new LoginResultado(token);
    }

    @Override
    public Class<LoginComando> tipoDeSolicitud() {
        return LoginComando.class;
    }
}
