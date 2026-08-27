package com.banco.batch.config;

import com.banco.batch.decider.TransaccionResultadoDecider;
import com.banco.batch.listener.LoggingJobExecutionListener;
import com.banco.batch.listener.LoggingSkipListener;
import com.banco.batch.listener.LoggingStepExecutionListener;
import com.banco.batch.model.Transaccion;
import com.banco.batch.policy.TransaccionSkipPolicy;
import com.banco.batch.processor.TransaccionProcessor;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
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
public class TransaccionesBatchConfig{
    @Bean
    public FlatFileItemReader<Transaccion> transaccionFileReader() {
        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionFileReader")
                .resource(new ClassPathResource("data/transacciones.csv"))
                .linesToSkip(1) // salta el encabezado
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(fieldSet -> {
                    Transaccion t = new Transaccion();
                    t.setId(fieldSet.readLong("id"));
                    t.setFecha(LocalDate.parse(fieldSet.readString("fecha")));
                    t.setMonto(fieldSet.readDouble("monto"));
                    t.setTipo(fieldSet.readString("tipo"));
                    return t;
                })
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<Transaccion> transaccionReader(){
        return new SynchronizedItemStreamReader<>(transaccionFileReader());
    }

    @Bean
    public TransaccionProcessor transaccionProcessor(){
        return new TransaccionProcessor();
    }

    @Bean
    public JpaItemWriter<Transaccion> transaccionWriter(EntityManagerFactory emf){
        return new JpaItemWriterBuilder<Transaccion>()
            .entityManagerFactory(emf)
            .build();
    }

    @Bean
    public TaskExecutor transaccionTaskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setThreadNamePrefix("transaccion-hilo-");
        // Con 3 hilos fijos y chunks de 5, una cola de 10 absorbe picos sin crecer sin límite,
        // acorde al volumen de datos de este proyecto (archivos CSV pequeños).
        executor.setQueueCapacity(10);
        executor.initialize();
        return executor;
    }

    @Bean
    public LoggingSkipListener<Transaccion, Transaccion> transaccionSkipListener() {
        return new LoggingSkipListener<>();
    }

    @Bean
    public LoggingStepExecutionListener transaccionStepExecutionListener() {
        return new LoggingStepExecutionListener();
    }

    @Bean
    public LoggingJobExecutionListener loggingJobExecutionListener() {
        return new LoggingJobExecutionListener();
    }

    @Bean
    public Step transaccionStep(JobRepository jobRepository,
                                PlatformTransactionManager tx,
                                SynchronizedItemStreamReader<Transaccion> reader,
                                TransaccionProcessor processor,
                                JpaItemWriter<Transaccion> writer,
                                TaskExecutor transaccionTaskExecutor,
                                LoggingSkipListener<Transaccion, Transaccion> transaccionSkipListener,
                                LoggingStepExecutionListener transaccionStepExecutionListener) {
        return new StepBuilder("transaccionStep", jobRepository)
                .<Transaccion, Transaccion>chunk(5, tx)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .skipPolicy(new TransaccionSkipPolicy())
                .taskExecutor(transaccionTaskExecutor)
                .listener(transaccionSkipListener)
                .listener(transaccionStepExecutionListener)
                .build();
    }

    @Bean
    public TransaccionResultadoDecider transaccionResultadoDecider() {
        return new TransaccionResultadoDecider();
    }

    @Bean
    public Job transaccionesJob(JobRepository jobRepository,
                                Step transaccionStep,
                                TransaccionResultadoDecider transaccionResultadoDecider,
                                LoggingJobExecutionListener loggingJobExecutionListener) {
        return new JobBuilder("transaccionesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(loggingJobExecutionListener)
                .start(transaccionStep)
                .next(transaccionResultadoDecider)
                .on("OK").end()
                .from(transaccionResultadoDecider).on("ADVERTENCIA").end("COMPLETED WITH WARNINGS")
                .from(transaccionResultadoDecider).on("CRITICO").fail()
                .end()
                .build();
    }
}