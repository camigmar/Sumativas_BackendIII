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
- `policy`: los SkipPolicy custom de cada job (`TransaccionSkipPolicy`, `InteresSkipPolicy`, `CuentaAnualSkipPolicy`), cada uno con su propio criterio de qué excepciones perdonar.
- `partition`: el `Partitioner` custom que reparte `transaccionesJob` en particiones por rango de filas.
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

Elegimos dos técnicas de escalado distintas a propósito, para poder comparar sus resultados sobre datos reales.

`transaccionStep` usa particiones. Un `TransaccionPartitioner` divide el CSV en 3 rangos de filas (gridSize 3) y un `TaskExecutorPartitionHandler` corre cada rango como una ejecución de step independiente, en su propio hilo. Cada partición tiene su propio `FlatFileItemReader` (`@StepScope`, con `linesToSkip`/`maxItemCount` según su rango), así que a diferencia del multithreading no hay un reader compartido que sincronizar. Con las 10 filas de `transacciones.csv` el reparto quedó 4-3-3, con las 3 particiones corriendo en paralelo.

`interesStep` sigue con multithreading, pero ahora la cantidad de hilos y el chunk son configurables sin recompilar (`batch.intereses.hilos` y `batch.intereses.chunk` en `application.properties`, o por línea de comandos). Probamos el mismo dataset con 3 configuraciones distintas: con 1 hilo y chunk 5 el job tardó 543ms, con 3 hilos y el mismo chunk bajó a 269ms, y con 5 hilos y chunk 10 llegó a 240ms. La tendencia es clara: más hilos, menos tiempo. El valor por defecto en `application.properties` se actualizó a 5 hilos/chunk 10, la configuración más rápida encontrada en la comparación. `transaccionStep` y `movimientoStep` no se modificaron: el primero no mostró una diferencia real entre configuraciones con un CSV tan chico, y el segundo no fue parte de esta comparación, así que no había evidencia propia para justificar un cambio ahí.

`movimientoStep` (dentro de `cuentasAnualesJob`) sigue con multithreading fijo (3 hilos, `SynchronizedItemStreamReader`), sin cambios, no era parte de esta comparación. `informeAnualStep` sigue fuera de cualquier esquema paralelo por la misma razón de siempre: es una agregación JPQL sobre datos ya persistidos.

## Tolerancia a fallos

Usamos dos mecanismos distintos, para dos tipos de error distintos:

- **SkipPolicy** (omisión de datos inválidos): ahora los 3 Jobs tienen su propia policy en vez de perdonar cualquier `Exception`. `TransaccionSkipPolicy` e `InteresSkipPolicy` solo perdonan `FlatFileParseException` y `NumberFormatException`; `CuentaAnualSkipPolicy` agrega también `DateTimeParseException`, por los dos formatos de fecha que acepta el CSV de movimientos. El límite de omisiones de cada Job es configurable (`batch.transacciones.skip-limite`, `batch.intereses.skip-limite`, `batch.cuentas-anuales.skip-limite`), 20 por defecto en los tres.
- **RetryPolicy** (fallos transitorios de infraestructura): los tres steps reintentan ante un `TransientDataAccessException` (por ejemplo, un deadlock momentáneo de MySQL), con el límite de reintentos también externalizado (`batch.*.retry-limite`, 3 por defecto). A diferencia del skip, esto no descarta el dato, reintenta la misma operación porque el error no depende del contenido del registro.

## Control de finalización

`transaccionesJob` termina de forma distinta según cuántos registros se saltearon en `transaccionStep`, a través de un `JobExecutionDecider` (`TransaccionResultadoDecider`):

Como `transaccionStep` ahora corre particionado, el decider no lee el `skipCount` de un solo step: suma el de todas las ejecuciones cuyo nombre empieza con `transaccionStep` (las 3 particiones), para no perder de vista omisiones que ocurran en cualquiera de ellas.

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

Para `interesesJob`, los hilos y el chunk se pueden ajustar sin recompilar agregando parámetros, por ejemplo: `--batch.intereses.hilos=5 --batch.intereses.chunk=10`.

Cada job usa `RunIdIncrementer`, así que se puede correr las veces que quieras sin que Spring Batch se queje de una instancia ya completada — eso sí, significa que si un job falla a mitad de camino, la próxima corrida no retoma desde ahí, arranca de cero con un `run.id` nuevo. Lo dejamos así a propósito para poder repetir las pruebas libremente; la capacidad de Spring Batch de reanudar un job fallido sigue disponible de fondo (el estado se persiste en MySQL vía `JobRepository`), solo que no la estamos usando con parámetros fijos.

## BFF Móvil y BFF Cajero

Además del BFF Web, el proyecto expone dos backends adicionales pensados para los otros canales del Banco XYZ, cada uno con el nivel de detalle y las validaciones que le corresponden.

### BFF Móvil (`com.banco.bff.movil`)

Pensado para minimizar el consumo de datos de la app: los DTOs solo traen los campos esenciales (sin el desglose de estado de cuenta anual que sí expone el BFF Web) y el listado de movimientos se acota a los últimos 10.

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/movil/cuentas/{cuentaId}` | Datos esenciales de la cuenta: nombre, saldo, tipo |
| GET | `/api/movil/cuentas/{cuentaId}/movimientos` | Últimos 10 movimientos (fecha, transacción, monto) |

Ejemplo:
```
GET /api/movil/cuentas/101
```
```json
{
  "cuentaId": 101,
  "nombre": "Juan Pérez",
  "saldo": 15000.0,
  "tipo": "ahorro"
}
```

### BFF Cajero (`com.banco.bff.cajero`)

Pensado para operaciones críticas de un cajero automático: consulta de saldo y retiro, con validaciones estrictas antes de mover cualquier dinero (monto positivo, cuenta existente, saldo suficiente).

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/cajero/cuentas/{cuentaId}/saldo` | Devuelve solo el saldo actual |
| GET | `/api/cajero/cuentas/{cuentaId}/validar` | Confirma si la cuenta existe (200/404) |
| POST | `/api/cajero/cuentas/{cuentaId}/retiro` | Retira un monto y descuenta el saldo |

Ejemplo de retiro:
```
POST /api/cajero/cuentas/101/retiro
Content-Type: application/json

{ "monto": 5000 }
```
```json
{
  "cuentaId": 101,
  "montoRetirado": 5000.0,
  "saldoRestante": 10000.0
}
```
Si el saldo no alcanza, responde `409 Conflict` con `{ "mensaje": "Saldo insuficiente para realizar el retiro" }`.

## Evidencia de ejecución

La evidencia de ejecución (logs y capturas de cada Job corriendo) se entrega en un documento aparte dentro de la carpeta del grupo.
