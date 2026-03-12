package btg_pactual_v1.btg_pactual_v2.application.registro.command;

import btg_pactual_v1.btg_pactual_v2.builder.RegistroComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionConflicto;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Rol;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoHashContrasena;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroManejadorTest {

    private PuertoRepositorioCliente repositorioCliente;
    private PuertoHashContrasena hashContrasena;
    private RegistroManejador manejador;

    @BeforeEach
    void setUp() {
        repositorioCliente = mock(PuertoRepositorioCliente.class);
        hashContrasena = mock(PuertoHashContrasena.class);
        manejador = new RegistroManejador(repositorioCliente, hashContrasena);
    }

    @Test
    @DisplayName("Registro exitoso — retorna resultado con datos correctos y rol CLIENTE")
    void registroExitoso() {
        // Arrange
        RegistroComando comando = new RegistroComandoBuilder()
                .conEmail("carlos@email.com")
                .build();

        when(repositorioCliente.existePorEmail("carlos@email.com")).thenReturn(false);
        when(hashContrasena.hash("Pass1!")).thenReturn("$2a$10$hashed");
        when(repositorioCliente.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        RegistroResultado resultado = manejador.manejar(comando);

        // Assert
        assertEquals("Carlos Perez", resultado.nombre());
        assertEquals("carlos@email.com", resultado.email());
        assertEquals("CLIENTE", resultado.rol());
        verify(repositorioCliente).guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Email duplicado — lanza ExcepcionConflicto")
    void emailDuplicado() {
        // Arrange
        RegistroComando comando = new RegistroComandoBuilder()
                .conEmail("existente@email.com")
                .build();

        when(repositorioCliente.existePorEmail("existente@email.com")).thenReturn(true);

        // Act & Assert
        ExcepcionConflicto ex = assertThrows(ExcepcionConflicto.class,
                () -> manejador.manejar(comando));
        assertEquals("El email ya está registrado en el sistema", ex.getMessage());
        verify(repositorioCliente, never()).guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Saldo inicial correcto — el Cliente creado tiene el saldo del comando")
    void saldoInicialCorrecto() {
        // Arrange
        RegistroComando comando = new RegistroComandoBuilder()
                .conSaldoInicial(new BigDecimal("750000"))
                .build();

        when(repositorioCliente.existePorEmail(anyString())).thenReturn(false);
        when(hashContrasena.hash(anyString())).thenReturn("$2a$10$hashed");
        when(repositorioCliente.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        manejador.manejar(comando);

        // Assert
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repositorioCliente).guardar(captor.capture());
        assertEquals(new BigDecimal("750000"), captor.getValue().getSaldo());
    }

    @Test
    @DisplayName("Contraseña hasheada — se invoca PuertoHashContrasena y el hash se pasa al Cliente")
    void contrasenaHasheada() {
        // Arrange
        RegistroComando comando = new RegistroComandoBuilder()
                .conContrasena("Pass1!")
                .build();

        when(repositorioCliente.existePorEmail(anyString())).thenReturn(false);
        when(hashContrasena.hash("Pass1!")).thenReturn("$2a$10$elHashResultante");
        when(repositorioCliente.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        manejador.manejar(comando);

        // Assert
        verify(hashContrasena).hash("Pass1!");
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repositorioCliente).guardar(captor.capture());
        assertEquals("$2a$10$elHashResultante", captor.getValue().getContrasenaHash());
        assertEquals(Rol.CLIENTE, captor.getValue().getRol());
    }
}
