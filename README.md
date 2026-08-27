# Migración de Procesos Batch – Banco XYZ

Migramos tres procesos batch legacy del Banco XYZ a Spring Batch: el reporte de transacciones diarias, el cálculo de intereses mensuales y la generación de estados de cuenta anuales. Los datos de origen (con los errores típicos de un sistema legacy: montos en cero, saldos vacíos, edades fuera de rango, duplicados) vienen de [bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data).

## Tecnologías

- Java 17 (compila también con JDK 21/24, el bytecode target queda en 17)
- Spring Boot 4.1.0 + Spring Batch 6.0.4
- Spring Data JPA + Hibernate
- MySQL 8 (vía Docker)
- Maven

## Estructura del proyecto

El código está organizado por responsabilidad dentro de `com.banco.batch`:

- `model`: las entidades JPA (Transaccion, CuentaInteres, MovimientoAnual, EstadoCuentaAnual).
- `processor`: el ItemProcessor de cada job, donde vive la validación y transformación de datos.
- `reader`: el reader custom que arma el informe agregado de cuentas anuales.
- `policy`: el SkipPolicy custom del job de transacciones.
- `decider`: el JobExecutionDecider del job de transacciones.
- `listener`: los listeners que loguean skips, steps y jobs.
- `config`: la configuración de cada Job (reader, processor, writer, steps, paralelismo).

Los recursos (`application.properties` y los CSV de entrada) están en `src/main/resources`.

## Los 3 Jobs

Cada proceso legacy quedó como un Job independiente con su propio Reader, Processor y Writer.

`transaccionesJob` lee `transacciones.csv` con `TransaccionProcessor` y descarta las filas con monto nulo o en cero — por ejemplo, la fila `id=4` (monto 0) no pasa, mientras que la `id=3` (monto -200) sí, porque un débito negativo es un dato válido, no un error. El resultado se persiste en la tabla `transacciones` a través de un `JpaItemWriter`.

`interesesJob` lee `intereses.csv` con `InteresProcessor`, que descarta cuentas con saldo nulo/cero/negativo, edades fuera de 18-100 años, tipo de cuenta desconocido o registros duplicados. La cuenta `104` se cae por tener saldo 0, y la `106` por ser un duplicado exacto de la `101` (mismo nombre, saldo, edad y tipo). Las que pasan reciben su interés según el tipo de cuenta (ahorro 2%, préstamo 5%, hipoteca 3%) y quedan en `cuentas_interes`.

`cuentasAnualesJob` tiene dos steps. `movimientoStep` lee `cuentas_anuales.csv` con `MovimientoProcessor`, tolera fechas en dos formatos distintos y descarta movimientos sin descripción/tipo o con monto en cero (como la fila de la cuenta `107`), guardando lo válido en `movimientos_anuales`. `informeAnualStep` no toca el CSV: usa un reader propio (`InformeAnualReader`) que agrupa por cuenta los movimientos ya guardados y arma el resumen anual en `estados_cuenta_anual`.

## Escalamiento y procesamiento paralelo

Los tres steps de lectura (`transaccionStep`, `interesStep`, `movimientoStep`) corren en paralelo con 3 hilos (`ThreadPoolTaskExecutor`, `corePoolSize=3`, `maxPoolSize=3`, `queueCapacity=10`) y chunks de tamaño 5. El reader de cada uno está envuelto en `SynchronizedItemStreamReader` porque `FlatFileItemReader` no es thread-safe por sí solo.

`informeAnualStep` queda fuera de este esquema a propósito: es una agregación con una sola consulta JPQL sobre datos ya persistidos, no hay lectura de archivo que se beneficie de varios hilos.

## Tolerancia a fallos

Usamos dos mecanismos distintos, para dos tipos de error distintos:

- **SkipPolicy** (omisión de datos inválidos): en `transaccionesJob` hay un `TransaccionSkipPolicy` custom que permite hasta 20 omisiones, solo para errores de parseo/formato. En `interesesJob` y `cuentasAnualesJob` se usa `skipLimit(50).skip(Exception.class)`.
- **RetryPolicy** (fallos transitorios de infraestructura): los tres steps reintentan hasta 3 veces ante un `TransientDataAccessException` (por ejemplo, un deadlock momentáneo de MySQL). A diferencia del skip, esto no descarta el dato — reintenta la misma operación porque el error no depende del contenido del registro.

## Control de finalización

`transaccionesJob` termina de forma distinta según cuántos registros se saltearon en `transaccionStep`, a través de un `JobExecutionDecider` (`TransaccionResultadoDecider`):

- 0 omisiones → el job termina normal (`OK`).
- 1 a 5 omisiones → termina con advertencia (`COMPLETED WITH WARNINGS`).
- Más de 5 omisiones → el job se marca como fallido (`CRITICO`), para forzar una revisión manual del archivo fuente.

No implementamos un reintento automático de todo el step (como en algunos ejemplos de la guía) porque acá el origen es un CSV estático: si el step falla por datos malos, correrlo de nuevo produce el mismo resultado, no es un fallo transitorio.

## Listeners y logging

Cada step de lectura tiene un `SkipListener` que registra en log (nivel WARN) cada registro que se descarta y por qué, y un `StepExecutionListener` que loguea al terminar el step cuántos registros se leyeron, escribieron y saltaron, y cuánto tardó. Cada Job tiene además un `JobExecutionListener` que loguea el inicio y el resultado final. Esto es lo que revisamos para confirmar qué se leyó, qué se transformó y qué quedó persistido en cada corrida.

## Cómo ejecutar

### Prerrequisitos
- Java 17 (o superior; el proyecto compila con JDK 21/24 también)
- Docker

### 1. Levantar la base de datos

```
docker run --name banco-mysql -e MYSQL_ROOT_PASSWORD=NuevaClave123 -e MYSQL_DATABASE=banco_xyz -p 3306:3306 -d mysql:8
```

Si el contenedor ya existe, alcanza con `docker start banco-mysql`. La configuración de conexión está en `src/main/resources/application.properties` (usuario `root`, misma contraseña que arriba, base `banco_xyz`); las tablas se crean solas (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Compilar

```
./mvnw clean install
```

### 3. Ejecutar cada Job

```
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob"
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=interesesJob"
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=cuentasAnualesJob"
```

Cada job usa `RunIdIncrementer`, así que se puede correr las veces que quieras sin que Spring Batch se queje de una instancia ya completada — eso sí, significa que si un job falla a mitad de camino, la próxima corrida no retoma desde ahí, arranca de cero con un `run.id` nuevo. Lo dejamos así a propósito para poder repetir las pruebas libremente; la capacidad de Spring Batch de reanudar un job fallido sigue disponible de fondo (el estado se persiste en MySQL vía `JobRepository`), solo que no la estamos usando con parámetros fijos.

## Evidencia de ejecución

La evidencia de ejecución (logs y capturas de cada Job corriendo) se entrega en un documento aparte dentro de la carpeta del grupo.
