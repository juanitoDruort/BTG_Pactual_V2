package btg_pactual_v1.btg_pactual_v2.application.registro.command;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionConflicto;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Rol;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoHashContrasena;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;

import btg_pactual_v1.btg_pactual_v2.application.mediador.Manejador;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component("registroManejador")
public class RegistroManejador implements Manejador<RegistroComando, RegistroResultado> {

    private final PuertoRepositorioCliente repositorioCliente;
    private final PuertoHashContrasena hashContrasena;

    public RegistroManejador(PuertoRepositorioCliente repositorioCliente,
                             PuertoHashContrasena hashContrasena) {
        this.repositorioCliente = repositorioCliente;
        this.hashContrasena = hashContrasena;
    }

    @Override
    public RegistroResultado manejar(RegistroComando comando) {
        if (repositorioCliente.existePorEmail(comando.email())) {
            throw new ExcepcionConflicto("El email ya está registrado en el sistema");
        }

        String contrasenaHasheada = hashContrasena.hash(comando.contrasena());

        Cliente cliente = new Cliente(
                UUID.randomUUID().toString(),
                comando.nombre(),
                comando.saldoInicial(),
                comando.email(),
                comando.telefono(),
                comando.documentoIdentidad(),
                contrasenaHasheada,
                Rol.CLIENTE,
                false,
                0,
                LocalDateTime.now()
        );

        Cliente clienteGuardado = repositorioCliente.guardar(cliente);

        return new RegistroResultado(
                clienteGuardado.getId(),
                clienteGuardado.getNombre(),
                clienteGuardado.getEmail(),
                clienteGuardado.getRol().name()
        );
    }

    @Override
    public Class<RegistroComando> tipoDeSolicitud() {
        return RegistroComando.class;
    }
}
