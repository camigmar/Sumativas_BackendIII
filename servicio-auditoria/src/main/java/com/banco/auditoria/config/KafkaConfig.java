package com.banco.auditoria.config;

import com.banco.auditoria.exception.EventoInvalidoException;
import com.banco.auditoria.listener.AlertaAuditoriaListener;
import com.banco.auditoria.listener.RetiroAuditoriaListener;
import com.banco.auditoria.listener.TransaccionAuditoriaListener;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    private static final String SUFIJO_DLT = ".DLT";

    // Spring Boot asigna este handler a los @KafkaListener. Un error se reintenta (3 intentos en total,
    // con 1 s entre cada uno) y luego el mensaje va a <topico>.DLT en la misma particion, y el consumo
    // sigue con el siguiente. EventoInvalidoException no se reintenta: un JSON invalido no se arregla solo.
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, KafkaConfig::destinoDlt);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        errorHandler.addNotRetryableExceptions(EventoInvalidoException.class);
        return errorHandler;
    }

    // Destino explicito: el recoverer de esta version de Spring Kafka usa "<topico>-dlt" por defecto,
    // y ese topico lo crearia el broker con 1 sola particion.
    static TopicPartition destinoDlt(ConsumerRecord<?, ?> registro, Exception ex) {
        return new TopicPartition(registro.topic() + SUFIJO_DLT, registro.partition());
    }

    // Los topicos de origen tambien se declaran aqui (con la misma definicion que en sus productores) porque
    // auditoria puede arrancar antes que pagos: asi no quedan creados automaticamente con 1 sola particion.
    // Los .DLT necesitan al menos las mismas particiones que su origen, porque el recoverer usa la misma.
    @Bean
    public KafkaAdmin.NewTopics topicosAuditados() {
        return new KafkaAdmin.NewTopics(
                topico(RetiroAuditoriaListener.TOPICO),
                topico(TransaccionAuditoriaListener.TOPICO),
                topico(AlertaAuditoriaListener.TOPICO),
                topico(RetiroAuditoriaListener.TOPICO + SUFIJO_DLT),
                topico(TransaccionAuditoriaListener.TOPICO + SUFIJO_DLT),
                topico(AlertaAuditoriaListener.TOPICO + SUFIJO_DLT));
    }

    private NewTopic topico(String nombre) {
        return TopicBuilder.name(nombre).partitions(2).replicas(1).build();
    }
}
