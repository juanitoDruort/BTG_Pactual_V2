package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.security;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionEncriptacion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoEncriptacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AdaptadorEncriptacionAes implements PuertoEncriptacion {

    private static final String ALGORITMO = "AES";
    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int IV_LONGITUD = 12;
    private static final int TAG_LONGITUD_BITS = 128;

    private final SecretKeySpec claveSecreta;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdaptadorEncriptacionAes(@Value("${encriptacion.clave-aes}") String claveBase64) {
        byte[] claveBytes = Base64.getDecoder().decode(claveBase64);
        if (claveBytes.length != 32) {
            throw new IllegalArgumentException(
                    "La clave AES debe tener 256 bits (32 bytes). Recibidos: " + claveBytes.length);
        }
        this.claveSecreta = new SecretKeySpec(claveBytes, ALGORITMO);
    }

    @Override
    public String encriptar(String dato) {
        try {
            byte[] iv = new byte[IV_LONGITUD];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, claveSecreta, new GCMParameterSpec(TAG_LONGITUD_BITS, iv));

            byte[] textoCifrado = cipher.doFinal(dato.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, resultado, iv.length, textoCifrado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new ExcepcionEncriptacion("Error al encriptar dato");
        }
    }

    @Override
    public String desencriptar(String datoEncriptado) {
        try {
            byte[] datos = Base64.getDecoder().decode(datoEncriptado);

            byte[] iv = new byte[IV_LONGITUD];
            System.arraycopy(datos, 0, iv, 0, IV_LONGITUD);

            byte[] textoCifrado = new byte[datos.length - IV_LONGITUD];
            System.arraycopy(datos, IV_LONGITUD, textoCifrado, 0, textoCifrado.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, claveSecreta, new GCMParameterSpec(TAG_LONGITUD_BITS, iv));

            byte[] textoPlano = cipher.doFinal(textoCifrado);

            return new String(textoPlano, StandardCharsets.UTF_8);
        } catch (ExcepcionEncriptacion e) {
            throw e;
        } catch (Exception e) {
            throw new ExcepcionEncriptacion("Error al desencriptar dato");
        }
    }
}
