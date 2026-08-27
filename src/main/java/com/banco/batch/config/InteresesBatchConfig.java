package com.banco.batch.config;

import com.banco.batch.listener.LoggingJobExecutionListener;
import com.banco.batch.listener.LoggingSkipListener;
import com.banco.batch.listener.LoggingStepExecutionListener;
import com.banco.batch.model.CuentaInteres;
import com.banco.batch.processor.InteresProcessor;
import jakarta.persistence.EntityManagerFactory;
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
public class InteresesBatchConfig {

    @Bean
    public FlatFileItemReader<CuentaInteres> interesFileReader() {
        return new FlatFileItemReaderBuilder<CuentaInteres>()
                .name("interesFileReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(fieldSet -> {
                    CuentaInteres c = new CuentaInteres();
                    c.setCuentaId(fieldSet.readLong("cuenta_id"));
                    c.setNombre(fieldSet.readString("nombre"));
                    c.setSaldo(fieldSet.readDouble("saldo"));
                    c.setEdad(fieldSet.readInt("edad"));
                    c.setTipo(fieldSet.readString("tipo"));
                    return c;
                })
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<CuentaInteres> interesReader() {
        return new SynchronizedItemStreamReader<>(interesFileReader());
    }

    @Bean
    public InteresProcessor interesProcessor() {
        return new InteresProcessor();
    }

    @Bean
    public JpaItemWriter<CuentaInteres> interesWriter(EntityManagerFactory emf) {
        return new JpaItemWriterBuilder<CuentaInteres>()
                .entityManagerFactory(emf)
                .build();
    }

    @Bean
    public TaskExecutor interesTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setThreadNamePrefix("interes-hilo-");
        // Con 3 hilos fijos y chunks de 5, una cola de 10 absorbe picos sin crecer sin límite,
        // acorde al volumen de datos de este proyecto (archivos CSV pequeños).
        executor.setQueueCapacity(10);
        executor.initialize();
        return executor;
    }

    @Bean
    public LoggingSkipListener<CuentaInteres, CuentaInteres> interesSkipListener() {
        return new LoggingSkipListener<>();
    }

    @Bean
    public LoggingStepExecutionListener interesStepExecutionListener() {
        return new LoggingStepExecutionListener();
    }

    @Bean
    public Step interesStep(JobRepository jobRepository,
                             PlatformTransactionManager tx,
                             SynchronizedItemStreamReader<CuentaInteres> interesReader,
                             InteresProcessor interesProcessor,
                             JpaItemWriter<CuentaInteres> interesWriter,
                             TaskExecutor interesTaskExecutor,
                             LoggingSkipListener<CuentaInteres, CuentaInteres> interesSkipListener,
                             LoggingStepExecutionListener interesStepExecutionListener) {
        return new StepBuilder("interesStep", jobRepository)
                .<CuentaInteres, CuentaInteres>chunk(5, tx)
                .reader(interesReader)
                .processor(interesProcessor)
                .writer(interesWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .skipLimit(50)
                .skip(Exception.class)
                .taskExecutor(interesTaskExecutor)
                .listener(interesSkipListener)
                .listener(interesStepExecutionListener)
                .build();
    }

    @Bean
    public Job interesesJob(JobRepository jobRepository,
                            Step interesStep,
                            LoggingJobExecutionListener loggingJobExecutionListener) {
        return new JobBuilder("interesesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(loggingJobExecutionListener)
                .start(interesStep)
                .build();
    }
}
