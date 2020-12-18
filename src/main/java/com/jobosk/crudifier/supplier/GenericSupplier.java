package com.jobosk.crudifier.supplier;

import org.springframework.context.annotation.Bean;
import reactor.core.publisher.EmitterProcessor;

public abstract class GenericSupplier<T> {

    private EmitterProcessor<T> processor = EmitterProcessor.create();

    @Bean
    public EmitterProcessor<T> getProcessor() {
        return processor;
    }
}
