package com.banco.batch.writer;

import com.banco.batch.model.CuentaInteres;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

// El job de intereses arma cada CuentaInteres desde el CSV (sin estado ni clienteId) y el
// JpaItemWriter hace merge, que copia todos los campos sobre la fila existente. Antes de delegar,
// este writer copia estado y clienteId de la fila ya guardada para que un reproceso no los pise
// (por ejemplo, no reabre una cuenta CERRADA ni la desvincula de su cliente).
public class PreservarDatosCuentaWriter implements ItemWriter<CuentaInteres> {

    private final EntityManagerFactory entityManagerFactory;
    private final ItemWriter<CuentaInteres> delegado;

    public PreservarDatosCuentaWriter(EntityManagerFactory entityManagerFactory, ItemWriter<CuentaInteres> delegado) {
        this.entityManagerFactory = entityManagerFactory;
        this.delegado = delegado;
    }

    @Override
    public void write(Chunk<? extends CuentaInteres> items) throws Exception {
        // Mismo EntityManager transaccional que usa el JpaItemWriter dentro de la transaccion del chunk.
        EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (entityManager == null) {
            throw new IllegalStateException("No hay una transaccion activa para escribir el chunk");
        }
        for (CuentaInteres item : items) {
            CuentaInteres existente = entityManager.find(CuentaInteres.class, item.getCuentaId());
            if (existente != null) {
                item.setEstado(existente.getEstado());
                item.setClienteId(existente.getClienteId());
            }
        }
        delegado.write(items);
    }
}
