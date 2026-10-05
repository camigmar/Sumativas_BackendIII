package com.banco.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RetiroEventPublisher {

    public static final String TOPICO_RETIROS = "retiros-realizados";

    private static final Logger logger = LoggerFactory.getLogger(RetiroEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public RetiroEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    // El retiro ya quedo guardado cuando se llama a este metodo: cualquier falla al publicar
    // se registra en el log y no se propaga, para no devolver error al cliente ni revertir nada.
    public void publicar(RetiroRealizadoEvent evento) {
        try {
            String payload = jsonMapper.writeValueAsString(evento);
            kafkaTemplate.send(TOPICO_RETIROS, String.valueOf(evento.cuentaId()), payload)
                    .whenComplete((resultado, ex) -> {
                        if (ex != null) {
                            logger.error("No se pudo publicar RetiroRealizado de la cuenta {}: {}",
                                    evento.cuentaId(), ex.getMessage());
                        } else {
                            logger.info("RetiroRealizado publicado: cuenta={}, particion={}, offset={}",
                                    evento.cuentaId(),
                                    resultado.getRecordMetadata().partition(),
                                    resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            logger.error("No se pudo publicar RetiroRealizado de la cuenta {}: {}",
                    evento.cuentaId(), ex.getMessage());
        }
    }
}
