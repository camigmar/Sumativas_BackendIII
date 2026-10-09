package com.banco.pagos.service;

import com.banco.pagos.client.CuentasClient;
import com.banco.pagos.dto.DepositoRequestDTO;
import com.banco.pagos.dto.PagoResponseDTO;
import com.banco.pagos.dto.RetiroRequestDTO;
import com.banco.pagos.dto.TransferenciaRequestDTO;
import com.banco.pagos.exception.CuentaNoEncontradaException;
import com.banco.pagos.exception.CuentasNoDisponibleException;
import com.banco.pagos.exception.PagoFallidoException;
import com.banco.pagos.exception.SaldoInsuficienteException;
import com.banco.pagos.exception.TransferenciaInvalidaException;
import com.banco.pagos.model.EstadoPago;
import com.banco.pagos.model.Pago;
import com.banco.pagos.model.TipoPago;
import com.banco.pagos.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private CuentasClient cuentasClient;

    @InjectMocks
    private PagoService pagoService;

    // Estado del pago en cada save, en orden (el mock recibe siempre la misma instancia).
    private final List<EstadoPago> estadosGuardados = new ArrayList<>();
    private Pago pagoGuardado;

    private void simularGuardado() {
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocacion -> {
            Pago pago = invocacion.getArgument(0);
            pago.setId(1L);
            estadosGuardados.add(pago.getEstado());
            pagoGuardado = pago;
            return pago;
        });
    }

    @Test
    void retirar_casoExitosoQuedaCompletado() {
        simularGuardado();

        PagoResponseDTO respuesta = pagoService.retirar(new RetiroRequestDTO(101L, 200.0));

        verify(cuentasClient).debitar(101L, 200.0);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO);
        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.tipo()).isEqualTo(TipoPago.RETIRO);
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.COMPLETADO);
        assertThat(respuesta.cuentaDestino()).isNull();
    }

    @Test
    void retirar_saldoInsuficienteQuedaFallidoConMotivoY409() {
        simularGuardado();
        when(cuentasClient.debitar(101L, 900.0))
                .thenThrow(new SaldoInsuficienteException("Saldo insuficiente en la cuenta 101"));

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.retirar(new RetiroRequestDTO(101L, 900.0)));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.FALLIDO);
        assertThat(pagoGuardado.getMotivo()).contains("Saldo insuficiente");
    }

    @Test
    void depositar_casoExitosoQuedaCompletado() {
        simularGuardado();

        PagoResponseDTO respuesta = pagoService.depositar(new DepositoRequestDTO(102L, 300.0));

        verify(cuentasClient).acreditar(102L, 300.0);
        verify(cuentasClient, never()).debitar(any(), any());
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO);
        assertThat(respuesta.tipo()).isEqualTo(TipoPago.DEPOSITO);
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.COMPLETADO);
    }

    @Test
    void transferir_casoExitosoDebitaOrigenAcreditaDestinoYQuedaCompletado() {
        simularGuardado();

        PagoResponseDTO respuesta = pagoService.transferir(new TransferenciaRequestDTO(101L, 102L, 150.0));

        var orden = inOrder(cuentasClient);
        orden.verify(cuentasClient).debitar(101L, 150.0);
        orden.verify(cuentasClient).acreditar(102L, 150.0);
        orden.verifyNoMoreInteractions();
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO);
        assertThat(respuesta.cuentaOrigen()).isEqualTo(101L);
        assertThat(respuesta.cuentaDestino()).isEqualTo(102L);
    }

    @Test
    void transferir_creditoFallidoSeCompensaYQuedaCompensado() {
        simularGuardado();
        when(cuentasClient.acreditar(999L, 150.0))
                .thenThrow(new CuentaNoEncontradaException("La cuenta 999 no existe"));

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 999L, 150.0)));

        var orden = inOrder(cuentasClient);
        orden.verify(cuentasClient).debitar(101L, 150.0);
        orden.verify(cuentasClient).acreditar(999L, 150.0);
        orden.verify(cuentasClient).acreditar(101L, 150.0);
        assertThat(ex.getEstado()).isEqualTo(EstadoPago.COMPENSADO);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPENSADO);
        assertThat(pagoGuardado.getMotivo()).contains("La cuenta 999 no existe");
    }

    @Test
    void transferir_compensacionFallidaQuedaRequiereRevision() {
        simularGuardado();
        when(cuentasClient.acreditar(102L, 150.0))
                .thenThrow(new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null));
        when(cuentasClient.acreditar(101L, 150.0))
                .thenThrow(new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null));

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 102L, 150.0)));

        assertThat(ex.getEstado()).isEqualTo(EstadoPago.REQUIERE_REVISION);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.REQUIERE_REVISION);
        assertThat(pagoGuardado.getMotivo()).contains("La devolucion a la cuenta origen tambien fallo");
    }

    @Test
    void transferir_mismaCuentaLanzaExcepcionSinRegistrarNiLlamarACuentas() {
        assertThrows(TransferenciaInvalidaException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 101L, 150.0)));
        verifyNoInteractions(pagoRepository);
        verifyNoInteractions(cuentasClient);
    }
}
