package com.banco.batch.policy;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

public class TransaccionSkipPolicy implements SkipPolicy{
    private static final int MAX_SKIPS = 20;

    @Override
    public boolean shouldSkip(Throwable t, long skipCount){
        if(skipCount >= MAX_SKIPS){
            return false;
        }
        if(t instanceof FlatFileParseException || t instanceof NumberFormatException){
            return true;
        }
        return false;
    }
}
