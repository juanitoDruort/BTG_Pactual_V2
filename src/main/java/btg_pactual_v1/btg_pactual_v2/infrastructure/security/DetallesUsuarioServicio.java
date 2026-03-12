package btg_pactual_v1.btg_pactual_v2.infrastructure.security;

import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DetallesUsuarioServicio implements UserDetailsService {

    private final PuertoRepositorioCliente repositorioCliente;

    public DetallesUsuarioServicio(PuertoRepositorioCliente repositorioCliente) {
        this.repositorioCliente = repositorioCliente;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Cliente cliente = repositorioCliente.buscarPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return new User(
                cliente.getId(),
                cliente.getContrasenaHash(),
                !cliente.estaBloqueada(),  // enabled
                true,                       // accountNonExpired
                true,                       // credentialsNonExpired
                !cliente.estaBloqueada(),  // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + cliente.getRol().name()))
        );
    }
}
