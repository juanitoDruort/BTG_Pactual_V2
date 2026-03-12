package btg_pactual_v1.btg_pactual_v2.domain.port.out;

public interface PuertoGenerarToken {

    String generarToken(String userId, String rol);
}
