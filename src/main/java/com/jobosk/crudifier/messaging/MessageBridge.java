package com.jobosk.crudifier.messaging;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class MessageBridge<T> {

  private final EmitterProcessor<T> processor = EmitterProcessor.create();
  private final FluxSink<T> sink = processor.sink();

  public Flux<T> getMessageSupplier() {
    return this.processor;
  }

  public void pushMessage(final T message) {
    this.sink.next(message);
  }
}
