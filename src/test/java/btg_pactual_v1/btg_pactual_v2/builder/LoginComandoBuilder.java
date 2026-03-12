package btg_pactual_v1.btg_pactual_v2.builder;

import btg_pactual_v1.btg_pactual_v2.application.login.command.LoginComando;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoginComandoBuilder {

    private String email = "carlos.perez@email.com";
    private String contrasena = "Pass1!";

    public LoginComandoBuilder conEmail(String email) {
        this.email = email;
        return this;
    }

    public LoginComandoBuilder conContrasena(String contrasena) {
        this.contrasena = contrasena;
        return this;
    }

    public LoginComando build() {
        return new LoginComando(email, contrasena);
    }

    public String buildJson() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("email", email);
        map.put("contrasena", contrasena);
        return new ObjectMapper().writeValueAsString(map);
    }
}
