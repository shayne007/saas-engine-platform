package com.feng.engine.messaging;

/**
 * TODO
 *
 * @since 2025/8/11
 */
public abstract class AbstractMessageQueueAdapter {

    // Common interface that abstracts provider differences
    public abstract void send(UniversalMessage message);
    public abstract void subscribe(String topic, MessageHandler handler);
    public abstract void unsubscribe(String topic);
    public abstract MessageMetadata getMessageMetadata(String messageId);

    // Provider-specific capabilities
    protected abstract boolean supportsPartitioning();
    protected abstract boolean supportsTransactions();
    protected abstract boolean supportsMessageRetention();
    protected abstract boolean supportsBroadcast();

    // Capability mapping
    public final void sendWithOrdering(UniversalMessage message) {
        if (supportsPartitioning()) {
            sendWithPartitionKey(message);
        } else {
            // Fallback to single-threaded processing for ordering
            sendSequentially(message);
        }
    }

    protected abstract void sendWithPartitionKey(UniversalMessage message);
    protected abstract void sendSequentially(UniversalMessage message);
}

