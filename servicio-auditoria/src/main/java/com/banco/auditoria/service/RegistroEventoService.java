package com.banco.auditoria.service;

import com.banco.auditoria.exception.EventoInvalidoException;
import com.banco.auditoria.model.EventoAuditoria;
import com.banco.auditoria.repository.EventoAuditoriaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

@Service
public class RegistroEventoService {

    private static final Logger logger = LoggerFactory.getLogger(RegistroEventoService.class);

    private final EventoAuditoriaRepository repository;
    private final JsonMapper jsonMapper;

    public RegistroEventoService(EventoAuditoriaRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    // Valida el JSON, obtiene el tipo de evento con la funcion del listener y guarda el mensaje.
    // Devuelve false si el mensaje ya estaba guardado (reentrega). Lanza EventoInvalidoException si
    // el mensaje no se puede auditar.
    public boolean registrar(ConsumerRecord<String, String> registro, Function<Map<String, Object>, String> tipoEvento) {
        if (repository.existsByTopicoAndParticionAndOffset(registro.topic(), registro.partition(), registro.offset())) {
            logger.info("Mensaje ya auditado, se ignora: topico={}, particion={}, offset={}",
                    registro.topic(), registro.partition(), registro.offset());
            return false;
        }

        Map<String, Object> datos = leerJson(registro);
        String tipo = tipoEvento.apply(datos);
        if (tipo == null || tipo.isBlank()) {
            throw new EventoInvalidoException("El mensaje de " + registro.topic() + " no tiene los campos esperados: "
                    + registro.value(), null);
        }

        EventoAuditoria evento = new EventoAuditoria();
        evento.setTopico(registro.topic());
        evento.setTipoEvento(tipo);
        evento.setClave(registro.key());
        evento.setPayload(registro.value());
        evento.setParticion(registro.partition());
        evento.setOffset(registro.offset());
        evento.setFechaRegistro(LocalDateTime.now());
        try {
            repository.save(evento);
        } catch (DataIntegrityViolationException ex) {
            // Otra instancia (o un reintento) lo guardo entre la verificacion y el insert.
            logger.info("Mensaje ya auditado por otra entrega: topico={}, particion={}, offset={}",
                    registro.topic(), registro.partition(), registro.offset());
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leerJson(ConsumerRecord<String, String> registro) {
        if (registro.value() == null || registro.value().isBlank()) {
            throw new EventoInvalidoException("Mensaje vacio en " + registro.topic(), null);
        }
        try {
            return jsonMapper.readValue(registro.value(), Map.class);
        } catch (JacksonException ex) {
            throw new EventoInvalidoException("JSON invalido en " + registro.topic() + ": " + registro.value(), ex);
        }
    }

    // Extractores de tipo por topico.
    public static String tipoRetiro(Map<String, Object> datos) {
        return datos.get("cuentaId") != null ? "RETIRO_REALIZADO" : null;
    }

    public static String campoTipo(Map<String, Object> datos) {
        return datos.get("tipo") instanceof String tipo ? tipo : null;
    }
}
