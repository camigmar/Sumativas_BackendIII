package com.banco.batch.listener;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

public class LoggingStepExecutionListener implements StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggingStepExecutionListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("Inicia step {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Duration duracion = Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime());
        logger.info("Finaliza step {} - readCount={}, writeCount={}, skipCount={}, commitCount={}, duracion={}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                stepExecution.getCommitCount(),
                duracion);
        return stepExecution.getExitStatus();
    }
}
