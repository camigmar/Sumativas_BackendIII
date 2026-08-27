package com.banco.batch.listener;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

public class LoggingJobExecutionListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggingJobExecutionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Inicia job {} a las {}", jobExecution.getJobInstance().getJobName(), jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duracion = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
        logger.info("Finaliza job {} - status={}, duracion={}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                duracion);
    }
}
