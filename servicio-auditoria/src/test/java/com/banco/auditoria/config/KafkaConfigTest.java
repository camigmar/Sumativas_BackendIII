package com.banco.auditoria.config;

import com.banco.auditoria.exception.EventoInvalidoException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {

    @Test
    void mensajeFallido_vaAlTopicoPuntoDltEnLaMismaParticion() {
        ConsumerRecord<String, String> registro = new ConsumerRecord<>("transacciones-completadas", 1, 7L, null, "x");

        TopicPartition destino = KafkaConfig.destinoDlt(registro, new EventoInvalidoException("JSON invalido", null));

        assertThat(destino.topic()).isEqualTo("transacciones-completadas.DLT");
        assertThat(destino.partition()).isEqualTo(1);
    }
}
