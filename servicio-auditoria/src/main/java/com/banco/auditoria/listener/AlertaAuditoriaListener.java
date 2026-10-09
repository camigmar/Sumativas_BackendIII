package com.banco.auditoria.listener;

import com.banco.auditoria.service.RegistroEventoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertaAuditoriaListener {

    public static final String TOPICO = "alertas-seguridad";

    private static final Logger logger = LoggerFactory.getLogger(AlertaAuditoriaListener.class);

    private final RegistroEventoService registroEventoService;
    private final String instancia;

    public AlertaAuditoriaListener(RegistroEventoService registroEventoService,
                                   @Value("${HOSTNAME:local}") String instancia) {
        this.registroEventoService = registroEventoService;
        this.instancia = instancia;
    }

    // tipoEvento = tipo de alerta (OPERACION_RECHAZADA, COMPENSACION_EJECUTADA, REQUIERE_REVISION o MONTO_ELEVADO).
    @KafkaListener(topics = TOPICO)
    public void onAlertaSeguridad(ConsumerRecord<String, String> registro) {
        logger.warn("[AUDITORIA] instancia={} | topico={} | particion={} | offset={} | payload={}",
                instancia, registro.topic(), registro.partition(), registro.offset(), registro.value());
        registroEventoService.registrar(registro, RegistroEventoService::campoTipo);
    }
}
