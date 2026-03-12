package btg_pactual_v1.btg_pactual_v2.infrastructure.service;

import btg_pactual_v1.btg_pactual_v2.builder.ClienteBuilder;
import btg_pactual_v1.btg_pactual_v2.builder.SuscripcionBuilder;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionAccesoDenegado;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.model.Fondo;
import btg_pactual_v1.btg_pactual_v2.domain.model.Notificacion;
import btg_pactual_v1.btg_pactual_v2.domain.model.Suscripcion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoNotificacion;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioFondo;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioSuscripcion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioSuscripcionTest {

    @Mock
    private PuertoRepositorioFondo repositorioFondo;

    @Mock
    private PuertoRepositorioCliente repositorioCliente;

    @Mock
    private PuertoRepositorioSuscripcion repositorioSuscripcion;

    @Mock
    private PuertoNotificacion puertoNotificacion;

    @InjectMocks
    private ServicioSuscripcion servicio;

    @Test
    @DisplayName("cancelar — orquestación completa: buscar, validar, cancelar, abonar, guardar")
    void cancelarExitoso() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conId("sus-1")
                .conClienteId("cliente-1")
                .conFondoId("fondo-1")
                .conMonto(new BigDecimal("75000"))
                .conEstado(Suscripcion.Estado.ACTIVO)
                .build();

        Fondo fondo = new Fondo("fondo-1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV");

        Cliente cliente = new ClienteBuilder()
                .conId("cliente-1")
                .conSaldo(new BigDecimal("425000"))
                .build();

        when(repositorioSuscripcion.buscarPorId("sus-1")).thenReturn(Optional.of(suscripcion));
        when(repositorioFondo.buscarPorId("fondo-1")).thenReturn(Optional.of(fondo));
        when(repositorioCliente.buscarPorId("cliente-1")).thenReturn(Optional.of(cliente));

        // Act
        Suscripcion resultado = servicio.cancelar("sus-1", "cliente-1");

        // Assert
        assertEquals(Suscripcion.Estado.CANCELADO, resultado.getEstado());
        assertEquals(new BigDecimal("500000"), cliente.getSaldo());
        verify(repositorioCliente).guardar(cliente);
        verify(repositorioSuscripcion).guardar(suscripcion);
    }

    @Test
    @DisplayName("cancelar — suscripción no encontrada lanza ExcepcionDominio")
    void cancelarSuscripcionNoEncontrada() {
        // Arrange
        when(repositorioSuscripcion.buscarPorId("sus-inexistente")).thenReturn(Optional.empty());

        // Act & Assert
        ExcepcionDominio ex = assertThrows(ExcepcionDominio.class,
                () -> servicio.cancelar("sus-inexistente", "cliente-1"));
        assertEquals("Suscripción no encontrada: sus-inexistente", ex.getMessage());
    }

    @Test
    @DisplayName("cancelar — suscripción de otro cliente lanza ExcepcionAccesoDenegado")
    void cancelarSuscripcionDeOtroCliente() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conId("sus-1")
                .conClienteId("cliente-2")
                .build();

        when(repositorioSuscripcion.buscarPorId("sus-1")).thenReturn(Optional.of(suscripcion));

        // Act & Assert
        ExcepcionAccesoDenegado ex = assertThrows(ExcepcionAccesoDenegado.class,
                () -> servicio.cancelar("sus-1", "cliente-1"));
        assertEquals("No tiene permisos para cancelar esta suscripción", ex.getMessage());
    }

    @Test
    @DisplayName("cancelar — suscripción ya cancelada lanza ExcepcionDominio con nombre del fondo")
    void cancelarSuscripcionYaCancelada() {
        // Arrange
        Suscripcion suscripcion = new SuscripcionBuilder()
                .conId("sus-1")
                .conClienteId("cliente-1")
                .conFondoId("fondo-1")
                .conEstado(Suscripcion.Estado.CANCELADO)
                .build();

        Fondo fondo = new Fondo("fondo-1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV");

        when(repositorioSuscripcion.buscarPorId("sus-1")).thenReturn(Optional.of(suscripcion));
        when(repositorioFondo.buscarPorId("fondo-1")).thenReturn(Optional.of(fondo));

        // Act & Assert
        ExcepcionDominio ex = assertThrows(ExcepcionDominio.class,
                () -> servicio.cancelar("sus-1", "cliente-1"));
        assertEquals(
            "No se puede cancelar el FPV_BTG_PACTUAL_RECAUDADORA ya que no esta activo dentro de su portafolio",
            ex.getMessage()
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // suscribir — notificación
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("suscribir — envía notificación con datos correctos tras guardar")
    void suscribirEnviaNotificacion() {
        // Arrange
        Fondo fondo = new Fondo("fondo-1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV");
        Cliente cliente = new ClienteBuilder()
                .conId("cliente-1")
                .conEmail("juan@email.com")
                .conSaldo(new BigDecimal("500000"))
                .build();

        when(repositorioFondo.buscarPorId("fondo-1")).thenReturn(Optional.of(fondo));
        when(repositorioCliente.buscarPorId("cliente-1")).thenReturn(Optional.of(cliente));
        when(repositorioSuscripcion.existePorClienteIdYFondoId("cliente-1", "fondo-1")).thenReturn(false);
        when(repositorioSuscripcion.guardar(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        servicio.suscribir("cliente-1", "fondo-1", new BigDecimal("75000"));

        // Assert
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(puertoNotificacion).enviar(captor.capture());

        Notificacion notificacion = captor.getValue();
        assertEquals("juan@email.com", notificacion.destinatario());
        assertEquals(Notificacion.Canal.EMAIL, notificacion.canal());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", notificacion.nombreFondo());
        assertEquals(new BigDecimal("75000"), notificacion.monto());
    }

    @Test
    @DisplayName("suscribir — fallo de notificación NO revierte la suscripción (fire-and-forget)")
    void suscribirFireAndForget() {
        // Arrange
        Fondo fondo = new Fondo("fondo-1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV");
        Cliente cliente = new ClienteBuilder()
                .conId("cliente-1")
                .conSaldo(new BigDecimal("500000"))
                .build();

        when(repositorioFondo.buscarPorId("fondo-1")).thenReturn(Optional.of(fondo));
        when(repositorioCliente.buscarPorId("cliente-1")).thenReturn(Optional.of(cliente));
        when(repositorioSuscripcion.existePorClienteIdYFondoId("cliente-1", "fondo-1")).thenReturn(false);
        when(repositorioSuscripcion.guardar(any(Suscripcion.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Servicio de email caído")).when(puertoNotificacion).enviar(any());

        // Act
        Suscripcion resultado = servicio.suscribir("cliente-1", "fondo-1", new BigDecimal("75000"));

        // Assert — la suscripción se completó exitosamente a pesar del fallo de notificación
        assertEquals(Suscripcion.Estado.ACTIVO, resultado.getEstado());
        assertEquals("cliente-1", resultado.getClienteId());
        assertEquals(new BigDecimal("425000"), cliente.getSaldo());
    }
}
