package com.banco.auditoria.listener;

import com.banco.auditoria.exception.EventoInvalidoException;
import com.banco.auditoria.model.EventoAuditoria;
import com.banco.auditoria.repository.EventoAuditoriaRepository;
import com.banco.auditoria.service.RegistroEventoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaListenersTest {

    @Mock
    private EventoAuditoriaRepository repository;

    private RetiroAuditoriaListener retiroListener;
    private TransaccionAuditoriaListener transaccionListener;
    private AlertaAuditoriaListener alertaListener;

    @BeforeEach
    void setUp() {
        RegistroEventoService servicio = new RegistroEventoService(repository, JsonMapper.builder().build());
        retiroListener = new RetiroAuditoriaListener(servicio, "test");
        transaccionListener = new TransaccionAuditoriaListener(servicio, "test");
        alertaListener = new AlertaAuditoriaListener(servicio, "test");
    }

    private ConsumerRecord<String, String> registro(String topico, String valor) {
        return new ConsumerRecord<>(topico, 1, 42L, "101", valor);
    }

    private EventoAuditoria eventoGuardado() {
        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void retiro_guardaElEventoConSusMetadatos() {
        String payload = "{\"cuentaId\":101,\"monto\":100.0,\"nuevoSaldo\":400.0,\"fecha\":\"2026-10-09T10:00\"}";

        retiroListener.onRetiroRealizado(registro("retiros-realizados", payload));

        EventoAuditoria evento = eventoGuardado();
        assertThat(evento.getTopico()).isEqualTo("retiros-realizados");
        assertThat(evento.getTipoEvento()).isEqualTo("RETIRO_REALIZADO");
        assertThat(evento.getClave()).isEqualTo("101");
        assertThat(evento.getPayload()).isEqualTo(payload);
        assertThat(evento.getParticion()).isEqualTo(1);
        assertThat(evento.getOffset()).isEqualTo(42L);
        assertThat(evento.getFechaRegistro()).isNotNull();
    }

    @Test
    void transaccion_guardaConElTipoDelPago() {
        transaccionListener.onTransaccionCompletada(registro("transacciones-completadas",
                "{\"pagoId\":7,\"tipo\":\"TRANSFERENCIA\",\"cuentaOrigen\":101,\"cuentaDestino\":102,\"monto\":50.0}"));

        assertThat(eventoGuardado().getTipoEvento()).isEqualTo("TRANSFERENCIA");
    }

    @Test
    void alerta_guardaConElTipoDeAlerta() {
        alertaListener.onAlertaSeguridad(registro("alertas-seguridad",
                "{\"tipo\":\"MONTO_ELEVADO\",\"pagoId\":8,\"cuentaId\":101,\"monto\":20000.0}"));

        assertThat(eventoGuardado().getTipoEvento()).isEqualTo("MONTO_ELEVADO");
    }

    // Diseno de errores: el listener lanza EventoInvalidoException (no reintentable) y el DefaultErrorHandler
    // lo envia a <topico>.DLT; no se guarda nada y el contenedor sigue con el siguiente mensaje.
    @Test
    void jsonInvalido_lanzaEventoInvalidoSinGuardar() {
        assertThrows(EventoInvalidoException.class,
                () -> transaccionListener.onTransaccionCompletada(registro("transacciones-completadas", "esto no es json")));
        verify(repository, never()).save(any());
    }

    @Test
    void jsonSinCamposEsperados_lanzaEventoInvalidoSinGuardar() {
        assertThrows(EventoInvalidoException.class,
                () -> alertaListener.onAlertaSeguridad(registro("alertas-seguridad", "{\"otro\":1}")));
        assertThrows(EventoInvalidoException.class,
                () -> retiroListener.onRetiroRealizado(registro("retiros-realizados", "[1,2,3]")));
        assertThrows(EventoInvalidoException.class,
                () -> retiroListener.onRetiroRealizado(registro("retiros-realizados", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void mensajeYaAuditado_noSeGuardaDosVeces() {
        when(repository.existsByTopicoAndParticionAndOffset("retiros-realizados", 1, 42L)).thenReturn(true);

        retiroListener.onRetiroRealizado(registro("retiros-realizados", "{\"cuentaId\":101}"));

        verify(repository, never()).save(any());
    }

    @Test
    void choqueConOtraInstanciaAlGuardar_seIgnoraSinError() {
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("uk_evento_mensaje"));

        assertThatCode(() -> retiroListener.onRetiroRealizado(registro("retiros-realizados", "{\"cuentaId\":101}")))
                .doesNotThrowAnyException();
    }
}
