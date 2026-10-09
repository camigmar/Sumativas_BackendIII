package com.banco.pagos.event;

import com.banco.pagos.model.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Component
public class PagoEventPublisher {

    public static final String TOPICO_TRANSACCIONES = "transacciones-completadas";
    public static final String TOPICO_ALERTAS = "alertas-seguridad";

    private static final Logger logger = LoggerFactory.getLogger(PagoEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public PagoEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void publicarTransaccionCompletada(Pago pago) {
        publicar(TOPICO_TRANSACCIONES, pago.getCuentaOrigen(), new TransaccionCompletadaEvent(
                pago.getId(), pago.getTipo().name(), pago.getCuentaOrigen(), pago.getCuentaDestino(),
                pago.getMonto(), pago.getFecha().toString()));
    }

    public void publicarAlerta(TipoAlerta tipo, Pago pago, String detalle) {
        publicar(TOPICO_ALERTAS, pago.getCuentaOrigen(), new AlertaSeguridadEvent(
                tipo.name(), pago.getId(), pago.getCuentaOrigen(), pago.getMonto(), detalle,
                LocalDateTime.now().toString()));
    }

    // El pago ya quedo guardado cuando se llama a este metodo: cualquier falla al publicar
    // se registra en el log y no se propaga, para no devolver error al cliente ni alterar el pago.
    private void publicar(String topico, Long clave, Object evento) {
        try {
            String payload = jsonMapper.writeValueAsString(evento);
            kafkaTemplate.send(topico, String.valueOf(clave), payload)
                    .whenComplete((resultado, ex) -> {
                        if (ex != null) {
                            logger.error("No se pudo publicar en {} el evento {}: {}", topico, payload, ex.getMessage());
                        } else {
                            logger.info("Evento publicado en {}: clave={}, particion={}, offset={}",
                                    topico, clave,
                                    resultado.getRecordMetadata().partition(),
                                    resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            logger.error("No se pudo publicar en {} el evento {}: {}", topico, evento, ex.getMessage());
        }
    }
}
