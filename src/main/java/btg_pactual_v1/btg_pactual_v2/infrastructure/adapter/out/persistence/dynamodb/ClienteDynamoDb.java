package btg_pactual_v1.btg_pactual_v2.infrastructure.adapter.out.persistence.dynamodb;

import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Rol;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DynamoDbBean
public class ClienteDynamoDb {

    private String id;
    private String nombre;
    private String saldoEncriptado;
    private String email;
    private String telefono;
    private String documentoIdentidad;
    private String contrasenaHash;
    private String rol;
    private boolean cuentaBloqueada;
    private int intentosFallidosLogin;
    private String fechaRegistro;
    private String preferenciaNotificacion;

    public ClienteDynamoDb() {
    }

    @DynamoDbPartitionKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getSaldoEncriptado() { return saldoEncriptado; }
    public void setSaldoEncriptado(String saldoEncriptado) { this.saldoEncriptado = saldoEncriptado; }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isCuentaBloqueada() { return cuentaBloqueada; }
    public void setCuentaBloqueada(boolean cuentaBloqueada) { this.cuentaBloqueada = cuentaBloqueada; }

    public int getIntentosFallidosLogin() { return intentosFallidosLogin; }
    public void setIntentosFallidosLogin(int intentosFallidosLogin) { this.intentosFallidosLogin = intentosFallidosLogin; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getPreferenciaNotificacion() { return preferenciaNotificacion; }
    public void setPreferenciaNotificacion(String preferenciaNotificacion) { this.preferenciaNotificacion = preferenciaNotificacion; }

    public static ClienteDynamoDb fromDomain(Cliente cliente) {
        ClienteDynamoDb item = new ClienteDynamoDb();
        item.setId(cliente.getId());
        item.setNombre(cliente.getNombre());
        item.setSaldoEncriptado(null);
        item.setEmail(cliente.getEmail());
        item.setTelefono(cliente.getTelefono());
        item.setDocumentoIdentidad(cliente.getDocumentoIdentidad());
        item.setContrasenaHash(cliente.getContrasenaHash());
        item.setRol(cliente.getRol().name());
        item.setCuentaBloqueada(cliente.isCuentaBloqueada());
        item.setIntentosFallidosLogin(cliente.getIntentosFallidosLogin());
        item.setFechaRegistro(cliente.getFechaRegistro() != null
                ? cliente.getFechaRegistro().toString() : null);
        item.setPreferenciaNotificacion(null);
        return item;
    }

    public Cliente toDomain() {
        return new Cliente(
                id, nombre, BigDecimal.ZERO, email, telefono, documentoIdentidad,
                contrasenaHash, Rol.valueOf(rol),
                cuentaBloqueada, intentosFallidosLogin,
                fechaRegistro != null ? LocalDateTime.parse(fechaRegistro) : null
        );
    }
}
