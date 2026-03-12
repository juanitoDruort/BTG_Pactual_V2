package btg_pactual_v1.btg_pactual_v2.domain.port.out;

public interface PuertoHashContrasena {

    String hash(String textoPlano);

    boolean verificar(String textoPlano, String hash);
}
