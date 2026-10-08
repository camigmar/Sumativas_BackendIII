package com.banco.batch.reader;

import com.banco.batch.model.EstadoCuentaAnual;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.infrastructure.item.ItemReader;

public class InformeAnualReader implements ItemReader<EstadoCuentaAnual> {

    private final EntityManagerFactory emf;
    private Iterator<EstadoCuentaAnual> iterator;

    public InformeAnualReader(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public EstadoCuentaAnual read() throws Exception {
        if (iterator == null) {
            EntityManager em = emf.createEntityManager();
            List<Object[]> filas = em.createQuery(
                    "SELECT m.cuentaId, " +
                    "SUM(CASE WHEN m.monto > 0 THEN m.monto ELSE 0.0 END), " +
                    "SUM(CASE WHEN m.monto < 0 THEN m.monto ELSE 0.0 END), " +
                    "SUM(m.monto), COUNT(m) " +
                    "FROM MovimientoAnual m GROUP BY m.cuentaId", Object[].class)
                    .getResultList();
            em.close();

            List<EstadoCuentaAnual> estados = new ArrayList<>();
            for (Object[] fila : filas) {
                EstadoCuentaAnual e = new EstadoCuentaAnual();
                e.setCuentaId((Long) fila[0]);
                e.setTotalIngresos((Double) fila[1]);
                e.setTotalEgresos((Double) fila[2]);
                e.setSaldoNeto((Double) fila[3]);
                e.setCantidadMovimientos(((Long) fila[4]).intValue());
                estados.add(e);
            }
            iterator = estados.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
