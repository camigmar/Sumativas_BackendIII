package com.banco.batch.config;

import com.banco.batch.listener.LoggingJobExecutionListener;
import com.banco.batch.listener.LoggingSkipListener;
import com.banco.batch.listener.LoggingStepExecutionListener;
import com.banco.batch.model.EstadoCuentaAnual;
import com.banco.batch.model.MovimientoAnual;
import com.banco.batch.processor.MovimientoProcessor;
import com.banco.batch.reader.InformeAnualReader;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CuentasAnualesBatchConfig {

    @Bean
    public FlatFileItemReader<MovimientoAnual> movimientoFileReader() {
        return new FlatFileItemReaderBuilder<MovimientoAnual>()
                .name("movimientoFileReader")
                .resource(new ClassPathResource("data/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(fieldSet -> {
                    MovimientoAnual m = new MovimientoAnual();
                    m.setCuentaId(fieldSet.readLong("cuenta_id"));
                    String fechaStr = fieldSet.readString("fecha");
                    LocalDate fecha;
                    try {
                        fecha = LocalDate.parse(fechaStr);
                    } catch (DateTimeParseException e) {
                        fecha = LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    }
                    m.setFecha(fecha);
                    m.setTransaccion(fieldSet.readString("transaccion"));
                    m.setMonto(fieldSet.readDouble("monto"));
                    m.setDescripcion(fieldSet.readString("descripcion"));
                    return m;
                })
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<MovimientoAnual> movimientoReader() {
        return new SynchronizedItemStreamReader<>(movimientoFileReader());
    }

    @Bean
    public MovimientoProcessor movimientoProcessor() {
        return new MovimientoProcessor();
    }

    @Bean
    public JpaItemWriter<MovimientoAnual> movimientoWriter(EntityManagerFactory emf) {
        return new JpaItemWriterBuilder<MovimientoAnual>()
                .entityManagerFactory(emf)
                .build();
    }

    @Bean
    public TaskExecutor movimientoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setThreadNamePrefix("movimiento-hilo-");
        // Con 3 hilos fijos y chunks de 5, una cola de 10 absorbe picos sin crecer sin límite,
        // acorde al volumen de datos de este proyecto (archivos CSV pequeños).
        executor.setQueueCapacity(10);
        executor.initialize();
        return executor;
    }

    @Bean
    public LoggingSkipListener<MovimientoAnual, MovimientoAnual> movimientoSkipListener() {
        return new LoggingSkipListener<>();
    }

    @Bean
    public LoggingStepExecutionListener stepExecutionListener() {
        return new LoggingStepExecutionListener();
    }

    @Bean
    public Step movimientoStep(JobRepository jobRepository,
                                PlatformTransactionManager tx,
                                SynchronizedItemStreamReader<MovimientoAnual> movimientoReader,
                                MovimientoProcessor movimientoProcessor,
                                JpaItemWriter<MovimientoAnual> movimientoWriter,
                                TaskExecutor movimientoTaskExecutor,
                                LoggingSkipListener<MovimientoAnual, MovimientoAnual> movimientoSkipListener,
                                LoggingStepExecutionListener stepExecutionListener) {
        return new StepBuilder("movimientoStep", jobRepository)
                .<MovimientoAnual, MovimientoAnual>chunk(5, tx)
                .reader(movimientoReader)
                .processor(movimientoProcessor)
                .writer(movimientoWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .skipLimit(50)
                .skip(Exception.class)
                .taskExecutor(movimientoTaskExecutor)
                .listener(movimientoSkipListener)
                .listener(stepExecutionListener)
                .build();
    }

    @Bean
    public InformeAnualReader informeAnualReader(EntityManagerFactory emf) {
        return new InformeAnualReader(emf);
    }

    @Bean
    public JpaItemWriter<EstadoCuentaAnual> informeAnualWriter(EntityManagerFactory emf) {
        return new JpaItemWriterBuilder<EstadoCuentaAnual>()
                .entityManagerFactory(emf)
                .build();
    }

    // Sin paralelismo: InformeAnualReader agrega datos ya persistidos con una sola query JPQL,
    // no hay lectura de archivo que se beneficie de un pool de hilos.
    @Bean
    public Step informeAnualStep(JobRepository jobRepository,
                                  PlatformTransactionManager tx,
                                  InformeAnualReader informeAnualReader,
                                  JpaItemWriter<EstadoCuentaAnual> informeAnualWriter,
                                  LoggingStepExecutionListener stepExecutionListener) {
        return new StepBuilder("informeAnualStep", jobRepository)
                .<EstadoCuentaAnual, EstadoCuentaAnual>chunk(10, tx)
                .reader(informeAnualReader)
                .writer(informeAnualWriter)
                .listener(stepExecutionListener)
                .build();
    }

    @Bean
    public Job cuentasAnualesJob(JobRepository jobRepository,
                                 Step movimientoStep,
                                 Step informeAnualStep,
                                 LoggingJobExecutionListener loggingJobExecutionListener) {
        return new JobBuilder("cuentasAnualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(loggingJobExecutionListener)
                .start(movimientoStep)
                .next(informeAnualStep)
                .build();
    }
}
