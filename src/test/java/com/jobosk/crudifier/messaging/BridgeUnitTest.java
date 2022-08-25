package com.jobosk.crudifier.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;

class BridgeUnitTest {

  @Test
  void internalSink_isOk() {
    final EmitterProcessor<String> processor = EmitterProcessor.create();
    final FluxSink<String> sink = processor.sink();
    sink.next("test");
    assertEquals("test", processor.blockFirst(Duration.ofMillis(1000L)));
  }

  @Test
  void publishOne_isOk() {
    final MessageBridge<String> bridge = new MessageBridge<>();
    bridge.pushMessage("test");
    assertEquals("test", bridge.getMessageSupplier().blockFirst(Duration.ofMillis(1000L)));
  }

  @Test
  void publishMany_singleThread_isOk() {

    final MessageBridge<String> bridge = new MessageBridge<>();
    bridge.pushMessage("test1");
    bridge.pushMessage("test2");
    bridge.pushMessage("test3");

    final List<String> results = bridge.getMessageSupplier()
        .bufferTimeout(3, Duration.ofSeconds(5))
        .blockFirst();
    assertNotNull(results);
    assertEquals(3, results.size());
    assertEquals("test1", results.get(0));
    assertEquals("test2", results.get(1));
    assertEquals("test3", results.get(2));
  }

  @Test
  void publishMany_multiThread_isOk() {

    final MessageBridge<String> bridge = new MessageBridge<>();
    Stream.of("test1", "test2", "test3")
        .parallel()
        .forEach(bridge::pushMessage);

    final List<String> results = bridge.getMessageSupplier()
        .bufferTimeout(3, Duration.ofSeconds(5))
        .blockFirst();
    assertNotNull(results);
    assertEquals(3, results.size());
    assertTrue(results.contains("test1"));
    assertTrue(results.contains("test2"));
    assertTrue(results.contains("test3"));
  }
}
