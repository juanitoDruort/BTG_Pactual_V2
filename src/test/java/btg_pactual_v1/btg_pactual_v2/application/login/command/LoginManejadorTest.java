package btg_pactual_v1.btg_pactual_v2.application.login.command;

import btg_pactual_v1.btg_pactual_v2.builder.ClienteBuilder;
import btg_pactual_v1.btg_pactual_v2.builder.LoginComandoBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionCredencialesInvalidas;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionCuentaBloqueada;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoGenerarToken;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoHashContrasena;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginManejadorTest {

    private PuertoRepositorioCliente repositorioCliente;
    private PuertoHashContrasena hashContrasena;
    private PuertoGenerarToken generarToken;
    private LoginManejador manejador;

    @BeforeEach
    void setUp() {
        repositorioCliente = mock(PuertoRepositorioCliente.class);
        hashContrasena = mock(PuertoHashContrasena.class);
        generarToken = mock(PuertoGenerarToken.class);
        manejador = new LoginManejador(repositorioCliente, hashContrasena, generarToken, 3);
    }

    @Test
    @DisplayName("Login exitoso — retorna token JWT y resetea intentos")
    void loginExitoso() {
        // Arrange
        LoginComando comando = new LoginComandoBuilder().build();
        Cliente cliente = new ClienteBuilder()
                .conEmail("carlos.perez@email.com")
                .conIntentosFallidosLogin(1)
                .build();

        when(repositorioCliente.buscarPorEmail("carlos.perez@email.com")).thenReturn(Optional.of(cliente));
        when(hashContrasena.verificar("Pass1!", cliente.getContrasenaHash())).thenReturn(true);
        when(generarToken.generarToken(cliente.getId(), "CLIENTE")).thenReturn("jwt-token-123");
        when(repositorioCliente.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        LoginResultado resultado = manejador.manejar(comando);

        // Assert
        assertNotNull(resultado.token());
        assertEquals("jwt-token-123", resultado.token());

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repositorioCliente).guardar(captor.capture());
        assertEquals(0, captor.getValue().getIntentosFallidosLogin());
    }

    @Test
    @DisplayName("Email no encontrado — lanza ExcepcionCredencialesInvalidas")
    void emailNoEncontrado() {
        // Arrange
        LoginComando comando = new LoginComandoBuilder()
                .conEmail("inexistente@email.com")
                .build();

        when(repositorioCliente.buscarPorEmail("inexistente@email.com")).thenReturn(Optional.empty());

        // Act & Assert
        ExcepcionCredencialesInvalidas ex = assertThrows(ExcepcionCredencialesInvalidas.class,
                () -> manejador.manejar(comando));
        assertEquals("Credenciales inválidas", ex.getMessage());
        verify(repositorioCliente, never()).guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Contraseña incorrecta — incrementa intentos y lanza ExcepcionCredencialesInvalidas")
    void contrasenaIncorrecta() {
        // Arrange
        LoginComando comando = new LoginComandoBuilder().conContrasena("wrongPass").build();
        Cliente cliente = new ClienteBuilder()
                .conEmail("carlos.perez@email.com")
                .conIntentosFallidosLogin(0)
                .build();

        when(repositorioCliente.buscarPorEmail("carlos.perez@email.com")).thenReturn(Optional.of(cliente));
        when(hashContrasena.verificar("wrongPass", cliente.getContrasenaHash())).thenReturn(false);
        when(repositorioCliente.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        assertThrows(ExcepcionCredencialesInvalidas.class, () -> manejador.manejar(comando));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repositorioCliente).guardar(captor.capture());
        assertEquals(1, captor.getValue().getIntentosFallidosLogin());
    }

    @Test
    @DisplayName("Cuenta bloqueada — lanza ExcepcionCuentaBloqueada sin incrementar intentos")
    void cuentaBloqueada() {
        // Arrange
        LoginComando comando = new LoginComandoBuilder().build();
        Cliente cliente = new ClienteBuilder()
                .conEmail("carlos.perez@email.com")
                .conCuentaBloqueada(true)
                .build();

        when(repositorioCliente.buscarPorEmail("carlos.perez@email.com")).thenReturn(Optional.of(cliente));

        // Act & Assert
        ExcepcionCuentaBloqueada ex = assertThrows(ExcepcionCuentaBloqueada.class,
                () -> manejador.manejar(comando));
        assertEquals("La cuenta está bloqueada por múltiples intentos fallidos. Contacte al administrador", ex.getMessage());
        verify(hashContrasena, never()).verificar(anyString(), anyString());
        verify(repositorioCliente, never()).guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Tercer intento fallido — bloquea cuenta")
    void tercerIntentoFallidoBloqueaCuenta() {
        // Arrange
        LoginComando comando = new LoginComandoBuilder().conContrasena("wrongPass").build();
        Cliente cliente = new ClienteBuilder()
                .conEmail("carlos.perez@email.com")
                .conIntentosFallidosLogin(2)
                .build();

        when(repositorioCliente.buscarPorEmail("carlos.perez@email.com")).thenReturn(Optional.of(cliente));
        when(hashContrasena.verificar("wrongPass", cliente.getContrasenaHash())).thenReturn(false);
        when(repositorioCliente.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        assertThrows(ExcepcionCredencialesInvalidas.class, () -> manejador.manejar(comando));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repositorioCliente).guardar(captor.capture());
        assertEquals(3, captor.getValue().getIntentosFallidosLogin());
        assertTrue(captor.getValue().estaBloqueada());
    }
}
