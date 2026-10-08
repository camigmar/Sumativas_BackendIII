package com.banco.batch.policy;

import java.time.format.DateTimeParseException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

public class CuentaAnualSkipPolicy implements SkipPolicy{
    private final int maxSkips;

    public CuentaAnualSkipPolicy(int maxSkips){
        this.maxSkips = maxSkips;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount){
        if(skipCount >= maxSkips){
            return false;
        }
        if(t instanceof FlatFileParseException || t instanceof NumberFormatException || t instanceof DateTimeParseException){
            return true;
        }
        return false;
    }
}
