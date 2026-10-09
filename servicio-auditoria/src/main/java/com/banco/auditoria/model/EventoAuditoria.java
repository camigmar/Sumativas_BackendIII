package com.banco.auditoria.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// (topico, particion, offset) identifica un mensaje de Kafka: la restriccion unica evita guardarlo
// dos veces si se reentrega (reintento, rebalanceo entre instancias escaladas).
@Entity
@Table(name = "eventos_auditoria",
        uniqueConstraints = @UniqueConstraint(name = "uk_evento_mensaje", columnNames = {"topico", "particion", "offset_kafka"}))
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topico;

    @Column(nullable = false)
    private String tipoEvento;

    private String clave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Integer particion;

    // "offset" es palabra clave en SQL; la columna usa otro nombre.
    @Column(name = "offset_kafka", nullable = false)
    private Long offset;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTopico() {
        return topico;
    }
    public void setTopico(String topico) {
        this.topico = topico;
    }
    public String getTipoEvento() {
        return tipoEvento;
    }
    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }
    public String getClave() {
        return clave;
    }
    public void setClave(String clave) {
        this.clave = clave;
    }
    public String getPayload() {
        return payload;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }
    public Integer getParticion() {
        return particion;
    }
    public void setParticion(Integer particion) {
        this.particion = particion;
    }
    public Long getOffset() {
        return offset;
    }
    public void setOffset(Long offset) {
        this.offset = offset;
    }
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
