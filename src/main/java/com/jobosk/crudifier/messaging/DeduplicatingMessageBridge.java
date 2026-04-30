package com.jobosk.crudifier.messaging;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MessageBridge that, when invoked from inside a Spring-managed transaction,
 * buffers messages by a caller-provided key and emits a single message per key
 * on commit. Outside a transaction, or for messages excluded by the bufferable
 * predicate, behaves exactly like the parent MessageBridge.
 */
public class DeduplicatingMessageBridge<Message, Key> extends MessageBridge<Message> {

    private static final Logger log = Logger.getLogger(DeduplicatingMessageBridge.class.getName());

    private final Function<Message, Key> keyGetter;
    private final Predicate<Message> canBuffer;

    public DeduplicatingMessageBridge(final Function<Message, Key> keyGetter) {
        this(keyGetter, m -> true);
    }

    public DeduplicatingMessageBridge(final Function<Message, Key> keyGetter, final Predicate<Message> canBuffer) {
        this.keyGetter = keyGetter;
        this.canBuffer = canBuffer;
    }

    @Override
    public void pushMessage(final Message message) {
        if (message == null) {
            return;
        }
        if (canBuffer.test(message) && TransactionSynchronizationManager.isSynchronizationActive()) {
            getCurrentBuffer().put(keyGetter.apply(message), message);
            return;
        }
        super.pushMessage(message);
    }

    private Map<Key, Message> getCurrentBuffer() {
        @SuppressWarnings("unchecked")
        Map<Key, Message> existing = (Map<Key, Message>) TransactionSynchronizationManager.getResource(this);
        if (existing != null) {
            return existing;
        }
        final Map<Key, Message> created = new LinkedHashMap<>();
        TransactionSynchronizationManager.bindResource(this, created);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(final int status) {
                try {
                    if (TransactionSynchronizationManager.hasResource(DeduplicatingMessageBridge.this)) {
                        TransactionSynchronizationManager.unbindResource(DeduplicatingMessageBridge.this);
                    }
                    if (status == STATUS_COMMITTED) {
                        created.values().forEach(DeduplicatingMessageBridge.super::pushMessage);
                    }
                } catch (final Exception e) {
                    log.log(Level.SEVERE, "Error flushing message buffer", e);
                }
            }
        });
        return created;
    }
}
