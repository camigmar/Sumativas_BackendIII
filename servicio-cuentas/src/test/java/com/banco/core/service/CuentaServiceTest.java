package com.banco.core.service;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.repository.CuentaInteresRepository;
import com.banco.batch.repository.EstadoCuentaAnualRepository;
import com.banco.core.event.RetiroEventPublisher;
import com.banco.core.event.RetiroRealizadoEvent;
import com.banco.core.exception.CuentaNoEncontradaException;
import com.banco.core.exception.MontoInvalidoException;
import com.banco.core.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaInteresRepository cuentaInteresRepository;

    @Mock
    private EstadoCuentaAnualRepository estadoCuentaAnualRepository;

    @Mock
    private RetiroEventPublisher retiroEventPublisher;

    @InjectMocks
    private CuentaService cuentaService;

    @Test
    void retirar_montoNuloLanzaMontoInvalido() {
        assertThrows(MontoInvalidoException.class, () -> cuentaService.retirar(101L, null));
        verifyNoInteractions(cuentaInteresRepository);
        verifyNoInteractions(retiroEventPublisher);
    }

    @Test
    void retirar_montoCeroONegativoLanzaMontoInvalido() {
        assertThrows(MontoInvalidoException.class, () -> cuentaService.retirar(101L, 0.0));
        assertThrows(MontoInvalidoException.class, () -> cuentaService.retirar(101L, -50.0));
        verifyNoInteractions(cuentaInteresRepository);
        verifyNoInteractions(retiroEventPublisher);
    }

    @Test
    void retirar_cuentaNoEncontradaLanzaExcepcion() {
        when(cuentaInteresRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.retirar(999L, 100.0));
        verifyNoInteractions(retiroEventPublisher);
    }

    @Test
    void retirar_saldoInsuficienteLanzaExcepcion() {
        CuentaInteres cuenta = new CuentaInteres();
        cuenta.setCuentaId(101L);
        cuenta.setSaldo(50.0);
        when(cuentaInteresRepository.findById(101L)).thenReturn(Optional.of(cuenta));

        assertThrows(SaldoInsuficienteException.class, () -> cuentaService.retirar(101L, 100.0));
        verify(cuentaInteresRepository, never()).save(any());
        verifyNoInteractions(retiroEventPublisher);
    }

    @Test
    void retirar_casoExitosoDescuentaYGuardaElNuevoSaldo() {
        CuentaInteres cuenta = new CuentaInteres();
        cuenta.setCuentaId(101L);
        cuenta.setSaldo(500.0);
        when(cuentaInteresRepository.findById(101L)).thenReturn(Optional.of(cuenta));

        double nuevoSaldo = cuentaService.retirar(101L, 200.0);

        assertThat(nuevoSaldo).isEqualTo(300.0);
        assertThat(cuenta.getSaldo()).isEqualTo(300.0);
        verify(cuentaInteresRepository).save(cuenta);

        ArgumentCaptor<RetiroRealizadoEvent> captor = ArgumentCaptor.forClass(RetiroRealizadoEvent.class);
        verify(retiroEventPublisher).publicar(captor.capture());
        RetiroRealizadoEvent evento = captor.getValue();
        assertThat(evento.cuentaId()).isEqualTo(101L);
        assertThat(evento.monto()).isEqualTo(200.0);
        assertThat(evento.nuevoSaldo()).isEqualTo(300.0);
    }

    @Test
    void debitar_casoExitosoDescuentaGuardaYNoPublicaEvento() {
        CuentaInteres cuenta = new CuentaInteres();
        cuenta.setCuentaId(101L);
        cuenta.setSaldo(500.0);
        when(cuentaInteresRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(cuenta));

        double nuevoSaldo = cuentaService.debitar(101L, 200.0);

        assertThat(nuevoSaldo).isEqualTo(300.0);
        assertThat(cuenta.getSaldo()).isEqualTo(300.0);
        verify(cuentaInteresRepository).save(cuenta);
        verifyNoInteractions(retiroEventPublisher);
    }

    @Test
    void debitar_montoInvalidoLanzaExcepcion() {
        assertThrows(MontoInvalidoException.class, () -> cuentaService.debitar(101L, null));
        assertThrows(MontoInvalidoException.class, () -> cuentaService.debitar(101L, 0.0));
        assertThrows(MontoInvalidoException.class, () -> cuentaService.debitar(101L, -10.0));
        verifyNoInteractions(cuentaInteresRepository);
    }

    @Test
    void debitar_cuentaNoEncontradaLanzaExcepcion() {
        when(cuentaInteresRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.debitar(999L, 100.0));
        verify(cuentaInteresRepository, never()).save(any());
    }

    @Test
    void debitar_saldoInsuficienteLanzaExcepcionYNoGuarda() {
        CuentaInteres cuenta = new CuentaInteres();
        cuenta.setCuentaId(101L);
        cuenta.setSaldo(50.0);
        when(cuentaInteresRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(cuenta));

        assertThrows(SaldoInsuficienteException.class, () -> cuentaService.debitar(101L, 100.0));
        assertThat(cuenta.getSaldo()).isEqualTo(50.0);
        verify(cuentaInteresRepository, never()).save(any());
    }

    @Test
    void acreditar_casoExitosoSumaGuardaYNoPublicaEvento() {
        CuentaInteres cuenta = new CuentaInteres();
        cuenta.setCuentaId(102L);
        cuenta.setSaldo(100.0);
        when(cuentaInteresRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(cuenta));

        double nuevoSaldo = cuentaService.acreditar(102L, 250.0);

        assertThat(nuevoSaldo).isEqualTo(350.0);
        assertThat(cuenta.getSaldo()).isEqualTo(350.0);
        verify(cuentaInteresRepository).save(cuenta);
        verifyNoInteractions(retiroEventPublisher);
    }

    @Test
    void acreditar_montoInvalidoLanzaExcepcion() {
        assertThrows(MontoInvalidoException.class, () -> cuentaService.acreditar(102L, null));
        assertThrows(MontoInvalidoException.class, () -> cuentaService.acreditar(102L, 0.0));
        verifyNoInteractions(cuentaInteresRepository);
    }

    @Test
    void acreditar_cuentaNoEncontradaLanzaExcepcion() {
        when(cuentaInteresRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.acreditar(999L, 100.0));
        verify(cuentaInteresRepository, never()).save(any());
    }
}
