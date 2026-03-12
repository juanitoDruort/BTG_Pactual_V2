package btg_pactual_v1.btg_pactual_v2.domain.model;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Cliente {

    private final String id;
    private final String nombre;
    private BigDecimal saldo;
    private final String email;
    private final String telefono;
    private final String documentoIdentidad;
    private final String contrasenaHash;
    private final Rol rol;
    private boolean cuentaBloqueada;
    private int intentosFallidosLogin;
    private final LocalDateTime fechaRegistro;

    public Cliente(String id, String nombre, BigDecimal saldo, String email, String telefono,
                   String documentoIdentidad, String contrasenaHash, Rol rol,
                   boolean cuentaBloqueada, int intentosFallidosLogin, LocalDateTime fechaRegistro) {
        this.id = id;
        this.nombre = nombre;
        this.saldo = saldo;
        this.email = email;
        this.telefono = telefono;
        this.documentoIdentidad = documentoIdentidad;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.cuentaBloqueada = cuentaBloqueada;
        this.intentosFallidosLogin = intentosFallidosLogin;
        this.fechaRegistro = fechaRegistro;
    }

    public void descontarSaldo(BigDecimal monto, String nombreFondo) {
        if (this.saldo.compareTo(monto) < 0) {
            throw new ExcepcionDominio(
                "No tiene saldo disponible para vincularse al fondo " + nombreFondo + ". Saldo actual: " + this.saldo
            );
        }
        this.saldo = this.saldo.subtract(monto);
    }

    public void abonarSaldo(BigDecimal monto) {
        this.saldo = this.saldo.add(monto);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getSaldo() { return saldo; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public String getContrasenaHash() { return contrasenaHash; }
    public Rol getRol() { return rol; }
    public boolean isCuentaBloqueada() { return cuentaBloqueada; }
    public int getIntentosFallidosLogin() { return intentosFallidosLogin; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
