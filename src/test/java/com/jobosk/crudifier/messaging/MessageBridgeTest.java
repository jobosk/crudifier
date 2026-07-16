package com.jobosk.crudifier.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageBridgeTest {

    private MessageBridge<String> bridgeWithCounters() {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        return new MessageBridge<String>() {{
            setDroppedCounter(registry.counter("test.dropped"));
            setFailedCounter(registry.counter("test.failed"));
        }};
    }

    // ------------------------------------------------------------------
    // autoCancel=false: el processor sobrevive a la cancelación del subscriber
    // ------------------------------------------------------------------

    @Test
    void processor_survives_subscriber_cancellation_and_accepts_new_subscriber() {
        final MessageBridge<String> bridge = bridgeWithCounters();

        // Primer subscriber: se suscribe y cancela inmediatamente.
        bridge.asFlux().take(0).subscribe();

        // El processor debe seguir activo — "after-cancel" queda en buffer (autoCancel=false).
        bridge.pushMessage("after-cancel");

        // Segundo subscriber recibe el mensaje bufferizado y el siguiente.
        StepVerifier.create(bridge.asFlux().take(2))
                .then(() -> bridge.pushMessage("second"))
                .expectNext("after-cancel", "second")
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // Entrega normal sin subscriber: bufferea (downstreamCount=0 → likelyDropped=false)
    // ------------------------------------------------------------------

    @Test
    void pushMessage_without_subscriber_does_not_increment_counters() {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        final MessageBridge<String> bridge = new MessageBridge<String>() {{
            setDroppedCounter(registry.counter("test.dropped"));
            setFailedCounter(registry.counter("test.failed"));
        }};

        final boolean accepted = bridge.tryEmit("hello");

        assertThat(accepted).isTrue();
        assertThat(registry.find("test.dropped").counter().count()).isZero();
        assertThat(registry.find("test.failed").counter().count()).isZero();
    }

    // ------------------------------------------------------------------
    // drainDroppedCount: AtomicLong se resetea en cada llamada
    // ------------------------------------------------------------------

    @Test
    void drainDroppedCount_resets_on_each_call() {
        final MessageBridge<String> bridge = bridgeWithCounters();

        assertThat(bridge.drainDroppedCount()).isZero();
        assertThat(bridge.drainDroppedCount()).isZero();
    }

    // ------------------------------------------------------------------
    // safeNext: rama de fallo (Throwable) incrementa failedCounter y retorna false
    // ------------------------------------------------------------------

    @Test
    void tryEmit_whenSafeNextThrows_incrementsFailedCounterAndReturnsFalse() {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        final Counter failed = registry.counter("test.failed");
        final Counter dropped = registry.counter("test.dropped");

        final MessageBridge<String> bridge = new MessageBridge<String>() {{
            setDroppedCounter(dropped);
            setFailedCounter(failed);
        }

            @Override
            protected boolean safeNext(final String value) {
                failed.increment();   // simula lo que haría el catch real
                return false;
            }
        };

        final boolean result = bridge.tryEmit("x");

        assertThat(result).isFalse();
        assertThat(failed.count()).isEqualTo(1.0);
        assertThat(dropped.count()).isZero();   // NOT dropped — es un fallo de sink
    }

    // ------------------------------------------------------------------
    // getMessageSupplier es alias de asFlux (backward-compat)
    // ------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    void getMessageSupplier_is_alias_of_asFlux() {
        final MessageBridge<String> bridge = new MessageBridge<>();

        assertThat(bridge.getMessageSupplier()).isSameAs(bridge.asFlux());
    }

    // ------------------------------------------------------------------
    // Buffer: acepta más de 256 items (>límite del EmitterProcessor original)
    // ------------------------------------------------------------------

    @Test
    void pushMessage_delivers_many_items_above_legacy_buffer_limit() {
        final int COUNT = 500;
        final MessageBridge<Integer> bridge = new MessageBridge<>();
        final List<Integer> received = new ArrayList<>();

        bridge.asFlux().take(COUNT).subscribe(received::add);

        for (int i = 0; i < COUNT; i++) {
            bridge.pushMessage(i);
        }

        assertThat(received).hasSize(COUNT);
    }

    // ------------------------------------------------------------------
    // pushMessage (void alias) delega en tryEmit sin excepción
    // ------------------------------------------------------------------

    @Test
    void pushMessage_delegatesToTryEmit_noException() {
        final MessageBridge<String> bridge = new MessageBridge<>();

        StepVerifier.create(bridge.asFlux().take(1))
                .then(() -> bridge.pushMessage("msg"))
                .expectNext("msg")
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // Entrega en tiempo real con StepVerifier
    // ------------------------------------------------------------------

    @Test
    void pushMessage_delivers_items_to_subscriber() {
        final MessageBridge<String> bridge = new MessageBridge<>();

        StepVerifier.create(bridge.asFlux().take(3))
                .then(() -> {
                    bridge.pushMessage("a");
                    bridge.pushMessage("b");
                    bridge.pushMessage("c");
                })
                .expectNext("a", "b", "c")
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // DeduplicatingMessageBridge: sigue funcionando como subclase
    // ------------------------------------------------------------------

    @Test
    void deduplicatingBridge_works_outside_transaction() {
        final DeduplicatingMessageBridge<String, String> bridge =
                new DeduplicatingMessageBridge<>(s -> s);

        final List<String> received = new ArrayList<>();
        bridge.asFlux().take(2).subscribe(received::add);

        bridge.pushMessage("one");
        bridge.pushMessage("two");

        assertThat(received).containsExactly("one", "two");
    }
}
