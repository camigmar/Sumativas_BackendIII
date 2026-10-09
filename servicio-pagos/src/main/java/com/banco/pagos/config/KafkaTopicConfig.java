package com.banco.pagos.config;

import com.banco.pagos.event.PagoEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transaccionesCompletadasTopic() {
        return TopicBuilder.name(PagoEventPublisher.TOPICO_TRANSACCIONES)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic alertasSeguridadTopic() {
        return TopicBuilder.name(PagoEventPublisher.TOPICO_ALERTAS)
                .partitions(2)
                .replicas(1)
                .build();
    }
}
