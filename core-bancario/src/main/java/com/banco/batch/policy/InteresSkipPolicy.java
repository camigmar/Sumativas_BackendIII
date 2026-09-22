package com.banco.batch.policy;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

public class InteresSkipPolicy implements SkipPolicy{
    private final int maxSkips;

    public InteresSkipPolicy(int maxSkips){
        this.maxSkips = maxSkips;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount){
        if(skipCount >= maxSkips){
            return false;
        }
        if(t instanceof FlatFileParseException || t instanceof NumberFormatException){
            return true;
        }
        return false;
    }
}
