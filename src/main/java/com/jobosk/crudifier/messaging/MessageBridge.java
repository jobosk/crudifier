package com.jobosk.crudifier.messaging;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

public class MessageBridge<T> {

  private final EmitterProcessor<T> processor = EmitterProcessor.create();

  public Flux<T> getMessageSupplier() {
    return this.processor;
  }

  public void pushMessage(final T message) {
    this.processor.sink().next(message);
  }
}
