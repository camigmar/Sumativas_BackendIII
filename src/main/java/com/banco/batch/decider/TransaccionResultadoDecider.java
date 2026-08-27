package com.banco.batch.decider;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;

public class TransaccionResultadoDecider implements JobExecutionDecider {

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        long skipCount = stepExecution.getSkipCount();
        if (skipCount == 0) {
            return new FlowExecutionStatus("OK");
        } else if (skipCount <= 5) {
            return new FlowExecutionStatus("ADVERTENCIA");
        } else {
            return new FlowExecutionStatus("CRITICO");
        }
    }
}
