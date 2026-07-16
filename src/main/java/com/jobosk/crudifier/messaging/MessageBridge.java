package com.jobosk.crudifier.messaging;

import io.micrometer.core.instrument.Counter;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sink multicast reutilizable como respaldo de los {@code Supplier<Flux<T>>} de
 * Spring Cloud Stream.
 *
 * <h2>Problema que resuelve</h2>
 * <p>{@code EmitterProcessor.create()} sin parámetros tiene dos brechas críticas:
 * <ol>
 *   <li><b>autoCancel=true</b>: si el subscriber Kafka (producer de salida) cancela
 *       su suscripción (error irrecuperable, rebalance), el processor pasa a estado
 *       terminado. Todos los {@code sink.next()} posteriores se descartan vía
 *       {@code Operators.onNextDropped()} <em>sin excepción ni log</em>. El consumer
 *       de entrada sigue commiteando offsets → pérdida total del output, indetectable
 *       hasta que el lag del topic de salida deja de crecer.</li>
 *   <li><b>Buffer ilimitado</b>: con {@code OverflowStrategy.BUFFER} y sin límite
 *       efectivo, una ráfaga masiva bajo backpressure materializa el buffer en heap
 *       hasta OOM.</li>
 * </ol>
 *
 * <h2>Correcciones aplicadas</h2>
 * <ul>
 *   <li>{@code EmitterProcessor.create(BUFFER_SIZE=8192, autoCancel=false)}: el
 *       processor sobrevive a la cancelación del subscriber.</li>
 *   <li>{@code OverflowStrategy.DROP}: overflow acotado con instrumentación.</li>
 *   <li>Counters Micrometer opcionales ({@link #setDroppedCounter},
 *       {@link #setFailedCounter}): observabilidad de pérdidas vía Prometheus.</li>
 * </ul>
 *
 * <h2>Aviso de compatibilidad</h2>
 * <p>{@code EmitterProcessor} está deprecated en Reactor 3.4 y eliminado en 3.5.
 * Esta clase está diseñada para Spring Boot 2.3.x (Hoxton.SR9, Reactor 3.3.x).
 * En una migración a Boot 2.5+ sustituir por {@code Sinks.many().multicast()
 * .onBackpressureBuffer(BUFFER_SIZE, false)} con {@code tryEmitNext()} y
 * {@code EmitFailureHandler}.
 */
public class MessageBridge<T> {

    private static final Logger log = Logger.getLogger(MessageBridge.class.getName());
    private static final int BUFFER_SIZE = 8192;

    private final EmitterProcessor<T> processor = EmitterProcessor.create(BUFFER_SIZE, false);
    private final FluxSink<T> sink = processor.sink(FluxSink.OverflowStrategy.DROP);
    private final AtomicLong droppedSinceLastTick = new AtomicLong(0);

    private volatile Counter droppedCounter;
    private volatile Counter failedCounter;

    /**
     * Vista read-only del processor. Usar en el {@code @Bean Supplier<Flux<T>>}
     * que registra Spring Cloud Stream.
     */
    public Flux<T> asFlux() {
        return processor;
    }

    /**
     * Alias de {@link #asFlux()} para compatibilidad con código existente.
     *
     * @deprecated Usar {@link #asFlux()}.
     */
    @Deprecated
    public Flux<T> getMessageSupplier() {
        return asFlux();
    }

    /**
     * Intento de emisión no bloqueante. Si el processor está terminado o el buffer
     * está lleno, el mensaje se pierde con instrumentación.
     *
     * @return {@code true} si la emisión probablemente fue aceptada.
     */
    public boolean tryEmit(final T value) {
        final boolean likelyDropped =
                processor.downstreamCount() > 0 && sink.requestedFromDownstream() <= 0;
        if (!safeNext(value)) {
            return false;
        }
        if (likelyDropped) {
            droppedSinceLastTick.incrementAndGet();
            final Counter c = droppedCounter;
            if (c != null) c.increment();
        }
        return !likelyDropped;
    }

    /**
     * Alias void de {@link #tryEmit} para uso en lambdas / method references.
     */
    public void pushMessage(final T message) {
        tryEmit(message);
    }

    /**
     * Wrap defensivo de {@code sink.next()}. {@code protected} para que los tests
     * de subclases puedan sobreescribirlo y forzar la rama de fallo.
     */
    protected boolean safeNext(final T value) {
        try {
            sink.next(value);
            return true;
        } catch (final Throwable t) {
            log.log(Level.SEVERE, "MessageBridge sink exception — message lost (sink terminated or cancelled)", t);
            final Counter c = failedCounter;
            if (c != null) c.increment();
            return false;
        }
    }

    protected void setDroppedCounter(final Counter counter) {
        this.droppedCounter = counter;
    }

    protected void setFailedCounter(final Counter counter) {
        this.failedCounter = counter;
    }

    public long drainDroppedCount() {
        return droppedSinceLastTick.getAndSet(0);
    }
}
