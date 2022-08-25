package com.jobosk.crudifier.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;

class BridgeUnitTest {

  @Test
  void shouldUseBackPressureBufferStrategy() {
    final EmitterProcessor<String> processor = EmitterProcessor.create();
    final FluxSink<String> sink = processor.sink();
    sink.next("test");
    assertEquals("test", processor.blockFirst(Duration.ofMillis(1000L)));
  }

  @Test
  void shouldPublishElementsIntoSink() {
    final MessageBridge<String> bridge = new MessageBridge<>();
    bridge.pushMessage("test");
    assertEquals("test", bridge.getMessageSupplier().blockFirst(Duration.ofMillis(1000L)));
  }
}
