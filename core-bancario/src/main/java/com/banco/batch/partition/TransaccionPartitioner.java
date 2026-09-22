package com.banco.batch.partition;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.ClassPathResource;

public class TransaccionPartitioner implements Partitioner {

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long totalFilas = contarFilasDeDatos();

        Map<String, ExecutionContext> particiones = new HashMap<>();
        long tamanioBase = totalFilas / gridSize;
        long resto = totalFilas % gridSize;

        long start = 0;
        for (int i = 0; i < gridSize; i++) {
            long tamanioParticion = tamanioBase + (i < resto ? 1 : 0);
            long end = start + tamanioParticion;

            ExecutionContext context = new ExecutionContext();
            context.putLong("start", start);
            context.putLong("end", end);
            particiones.put("partition" + i, context);

            start = end;
        }

        return particiones;
    }

    private long contarFilasDeDatos() {
        try (var lines = Files.lines(new ClassPathResource("data/transacciones.csv").getFile().toPath())) {
            return lines.count() - 1;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer data/transacciones.csv", e);
        }
    }
}
