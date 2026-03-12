package btg_pactual_v1.btg_pactual_v2.builder;

import btg_pactual_v1.btg_pactual_v2.application.registro.command.RegistroComando;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegistroComandoBuilder {

    private String nombre = "Carlos Perez";
    private String email = "carlos.perez@email.com";
    private String telefono = "3001112233";
    private String documentoIdentidad = "CC111222333";
    private String contrasena = "Pass1!";
    private BigDecimal saldoInicial = new BigDecimal("500000");

    public RegistroComandoBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public RegistroComandoBuilder conEmail(String email) {
        this.email = email;
        return this;
    }

    public RegistroComandoBuilder conTelefono(String telefono) {
        this.telefono = telefono;
        return this;
    }

    public RegistroComandoBuilder conDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
        return this;
    }

    public RegistroComandoBuilder conContrasena(String contrasena) {
        this.contrasena = contrasena;
        return this;
    }

    public RegistroComandoBuilder conSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
        return this;
    }

    public RegistroComando build() {
        return new RegistroComando(nombre, email, telefono, documentoIdentidad, contrasena, saldoInicial);
    }

    public String buildJson() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nombre", nombre);
        map.put("email", email);
        map.put("telefono", telefono);
        map.put("documentoIdentidad", documentoIdentidad);
        map.put("contrasena", contrasena);
        map.put("saldoInicial", saldoInicial);
        return new ObjectMapper().writeValueAsString(map);
    }
}
