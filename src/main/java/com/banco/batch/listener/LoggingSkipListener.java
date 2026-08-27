package com.banco.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

public class LoggingSkipListener<T, S> implements SkipListener<T, S> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingSkipListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        logger.warn("Skip en lectura: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(T item, Throwable t) {
        logger.warn("Skip en procesamiento del item {}: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(S item, Throwable t) {
        logger.warn("Skip en escritura del item {}: {}", item, t.getMessage());
    }
}
