package btg_pactual_v1.btg_pactual_v2.application.desbloqueo.command;

import btg_pactual_v1.btg_pactual_v2.builder.ClienteBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DesbloqueoManejadorTest {

    @Mock
    private PuertoRepositorioCliente repositorioCliente;

    @InjectMocks
    private DesbloqueoManejador manejador;

    @Test
    @DisplayName("200 - desbloqueo exitoso invoca desbloquearCuenta y guardar")
    void desbloqueoExitoso() {
        // Arrange
        Cliente cliente = new ClienteBuilder()
                .conId("c-1")
                .conEmail("carlos@email.com")
                .conCuentaBloqueada(true)
                .conIntentosFallidosLogin(3)
                .build();
        when(repositorioCliente.buscarPorId("c-1")).thenReturn(Optional.of(cliente));
        when(repositorioCliente.guardar(cliente)).thenReturn(cliente);

        // Act
        DesbloqueoResultado resultado = manejador.manejar(new DesbloqueoComando("c-1"));

        // Assert
        assertEquals("c-1", resultado.clienteId());
        assertEquals("carlos@email.com", resultado.email());
        assertEquals("Cuenta desbloqueada exitosamente", resultado.mensaje());
        verify(repositorioCliente).guardar(cliente);
    }

    @Test
    @DisplayName("422 - cliente no encontrado → lanza ExcepcionDominio")
    void clienteNoEncontrado() {
        // Arrange
        when(repositorioCliente.buscarPorId("no-existe")).thenReturn(Optional.empty());

        // Act & Assert
        ExcepcionDominio ex = assertThrows(ExcepcionDominio.class,
                () -> manejador.manejar(new DesbloqueoComando("no-existe")));
        assertEquals("Cliente no encontrado: no-existe", ex.getMessage());
    }
}
