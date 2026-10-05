package com.banco.auditoria.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RetiroAuditoriaListener {

    private static final Logger logger = LoggerFactory.getLogger(RetiroAuditoriaListener.class);

    private final String puerto;

    public RetiroAuditoriaListener(@Value("${server.port}") String puerto) {
        this.puerto = puerto;
    }

    @KafkaListener(topics = "retiros-realizados")
    public void onRetiroRealizado(ConsumerRecord<String, String> registro) {
        logger.info("[AUDITORIA] instancia=puerto {} | particion={} | offset={} | payload={}",
                puerto, registro.partition(), registro.offset(), registro.value());
    }
}
