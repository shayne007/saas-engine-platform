package com.feng.engine.messaging;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Jedis;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.StreamEntryID;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
@ConditionalOnProperty(name = "universal-mq.provider", havingValue = "redis")
public class RedisMessageQueueAdapter extends AbstractMessageQueueAdapter {
    private final JedisPool jedisPool;
    private final RedisMessageConverter messageConverter;
    private final Map<String, StreamConsumerThread> consumers;

    @Override
    public void send(UniversalMessage message) {
        try (Jedis jedis = jedisPool.getResource()) {
            String streamKey = getStreamKey(message.getTopic());
            Map<String, String> fields = messageConverter.toRedisFields(message);

            // Handle message expiration
            if (message.getTtl() > 0) {
                fields.put("__ttl", String.valueOf(message.getTtl()));
            }

            // Use XADD with auto-generated ID
            StreamEntryID id = jedis.xadd(streamKey, StreamEntryID.NEW_ENTRY, fields);

            // Set stream TTL if configured
            if (message.getTtl() > 0) {
                jedis.expire(streamKey, (int)(message.getTtl() / 1000));
            }

            handleSendSuccess(message, id);
        } catch (Exception e) {
            handleSendFailure(message, e);
        }
    }

    @Override
    public void subscribe(String topic, MessageHandler handler) {
        String streamKey = getStreamKey(topic);
        String consumerGroup = getConsumerGroup();
        String consumerName = generateConsumerName();

        // Create consumer group if not exists
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                jedis.xgroupCreate(streamKey, consumerGroup, StreamEntryID.LAST_ENTRY, false);
            } catch (JedisDataException e) {
                // Group already exists - ignore
                if (!e.getMessage().contains("BUSYGROUP")) {
                    throw e;
                }
            }
        }

        StreamConsumerThread consumerThread = new StreamConsumerThread(
            streamKey, consumerGroup, consumerName, handler
        );
        consumerThread.start();
        consumers.put(topic, consumerThread);
    }

    @Override
    protected boolean supportsPartitioning() { return false; }

    @Override
    protected boolean supportsTransactions() { return false; }

    @Override
    protected boolean supportsMessageRetention() { return true; }

    @Override
    protected boolean supportsBroadcast() { return true; }

    // Redis-specific: Simulate partitioning through multiple streams
    @Override
    protected void sendWithPartitionKey(UniversalMessage message) {
        String partitionedTopic = message.getTopic() + "_" +
            (Math.abs(message.getMessageKey().hashCode()) % getPartitionCount());
        UniversalMessage partitionedMessage = message.toBuilder()
            .topic(partitionedTopic)
            .build();
        send(partitionedMessage);
    }

    private class StreamConsumerThread extends Thread {
        private final String streamKey;
        private final String consumerGroup;
        private final String consumerName;
        private final MessageHandler handler;
        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {
                try (Jedis jedis = jedisPool.getResource()) {
                    // Read pending messages first
                    List<StreamPendingEntry> pending = jedis.xpending(streamKey, consumerGroup,
                        StreamEntryID.MINIMUM_ID, StreamEntryID.MAXIMUM_ID, 10, consumerName);

                    for (StreamPendingEntry entry : pending) {
                        processStreamEntry(jedis, entry.getID());
                    }

                    // Read new messages
                    Map<String, StreamEntryID> streams = Collections.singletonMap(
                        streamKey, StreamEntryID.UNRECEIVED_ENTRY);
                    List<Map.Entry<String, List<StreamEntry>>> result =
                        jedis.xreadGroup(consumerGroup, consumerName, 1, 1000, false, streams);

                    for (Map.Entry<String, List<StreamEntry>> streamEntry : result) {
                        for (StreamEntry entry : streamEntry.getValue()) {
                            processStreamEntry(jedis, entry.getID());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (Exception e) {
                    log.error("Error in Redis consumer", e);
                    try { Thread.sleep(1000); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        running = false;
                    }
                }
            }
        }

        private void processStreamEntry(Jedis jedis, StreamEntryID entryId) {
            try {
                List<StreamEntry> entries = jedis.xrange(streamKey, entryId, entryId);
                if (!entries.isEmpty()) {
                    UniversalMessage message = messageConverter.fromRedisEntry(entries.get(0));

                    // Check TTL
                    if (isMessageExpired(message)) {
                        jedis.xack(streamKey, consumerGroup, entryId);
                        return;
                    }

                    handler.handle(message);
                    jedis.xack(streamKey, consumerGroup, entryId);
                }
            } catch (Exception e) {
                log.error("Failed to process message", e);
                // Message will remain in pending list for retry
            }
        }

        public void shutdown() {
            running = false;
            interrupt();
        }
    }
}

