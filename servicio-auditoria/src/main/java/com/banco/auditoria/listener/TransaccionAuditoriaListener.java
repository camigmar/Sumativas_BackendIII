package com.banco.auditoria.listener;

import com.banco.auditoria.service.RegistroEventoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransaccionAuditoriaListener {

    public static final String TOPICO = "transacciones-completadas";

    private static final Logger logger = LoggerFactory.getLogger(TransaccionAuditoriaListener.class);

    private final RegistroEventoService registroEventoService;
    private final String instancia;

    public TransaccionAuditoriaListener(RegistroEventoService registroEventoService,
                                        @Value("${HOSTNAME:local}") String instancia) {
        this.registroEventoService = registroEventoService;
        this.instancia = instancia;
    }

    // tipoEvento = tipo del pago (DEPOSITO, RETIRO o TRANSFERENCIA).
    @KafkaListener(topics = TOPICO)
    public void onTransaccionCompletada(ConsumerRecord<String, String> registro) {
        logger.info("[AUDITORIA] instancia={} | topico={} | particion={} | offset={} | payload={}",
                instancia, registro.topic(), registro.partition(), registro.offset(), registro.value());
        registroEventoService.registrar(registro, RegistroEventoService::campoTipo);
    }
}
