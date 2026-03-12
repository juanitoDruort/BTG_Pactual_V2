package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.security;

import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoHashContrasena;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdaptadorHashBCrypt implements PuertoHashContrasena {

    private final PasswordEncoder passwordEncoder;

    public AdaptadorHashBCrypt(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String textoPlano) {
        return passwordEncoder.encode(textoPlano);
    }

    @Override
    public boolean verificar(String textoPlano, String hash) {
        return passwordEncoder.matches(textoPlano, hash);
    }
}
