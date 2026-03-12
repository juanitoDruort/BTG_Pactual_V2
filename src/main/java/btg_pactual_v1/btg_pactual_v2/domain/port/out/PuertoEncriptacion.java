package btg_pactual_v1.btg_pactual_v2.domain.port.out;

public interface PuertoEncriptacion {

    String encriptar(String dato);

    String desencriptar(String datoEncriptado);
}
