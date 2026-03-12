package btg_pactual_v1.btg_pactual_v2.builder;

import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Rol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClienteBuilder {

    private String id = "cliente-test";
    private String nombre = "Carlos Perez";
    private BigDecimal saldo = new BigDecimal("500000");
    private String email = "carlos.perez@email.com";
    private String telefono = "3001112233";
    private String documentoIdentidad = "CC111222333";
    private String contrasenaHash = "$2a$10$hashedPassword";
    private Rol rol = Rol.CLIENTE;
    private boolean cuentaBloqueada = false;
    private int intentosFallidosLogin = 0;
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public ClienteBuilder conId(String id) {
        this.id = id;
        return this;
    }

    public ClienteBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public ClienteBuilder conSaldo(BigDecimal saldo) {
        this.saldo = saldo;
        return this;
    }

    public ClienteBuilder conEmail(String email) {
        this.email = email;
        return this;
    }

    public ClienteBuilder conTelefono(String telefono) {
        this.telefono = telefono;
        return this;
    }

    public ClienteBuilder conDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
        return this;
    }

    public ClienteBuilder conContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
        return this;
    }

    public ClienteBuilder conRol(Rol rol) {
        this.rol = rol;
        return this;
    }

    public ClienteBuilder conCuentaBloqueada(boolean cuentaBloqueada) {
        this.cuentaBloqueada = cuentaBloqueada;
        return this;
    }

    public ClienteBuilder conIntentosFallidosLogin(int intentosFallidosLogin) {
        this.intentosFallidosLogin = intentosFallidosLogin;
        return this;
    }

    public ClienteBuilder conFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
        return this;
    }

    public Cliente build() {
        return new Cliente(id, nombre, saldo, email, telefono, documentoIdentidad,
                contrasenaHash, rol, cuentaBloqueada, intentosFallidosLogin, fechaRegistro);
    }
}
