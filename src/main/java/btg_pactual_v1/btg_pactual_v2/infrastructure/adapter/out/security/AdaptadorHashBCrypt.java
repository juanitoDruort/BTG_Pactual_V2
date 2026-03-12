package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.security;

import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoHashContrasena;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdaptadorHashBCrypt implements PuertoHashContrasena {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String textoPlano) {
        return passwordEncoder.encode(textoPlano);
    }
}
