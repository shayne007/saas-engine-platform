package com.feng.engine.messaging;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.requests.MetadataResponse.TopicMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
@ConditionalOnProperty(name = "universal-mq.provider", havingValue = "kafka")
public class KafkaMessageQueueAdapter extends AbstractMessageQueueAdapter {
    private final KafkaProducer<String, byte[]> producer;
    private final Map<String, KafkaConsumer<String, byte[]>> consumers;
    private final KafkaMessageConverter messageConverter;

    @Override
    public void send(UniversalMessage message) {
        ProducerRecord<String, byte[]> record = messageConverter.toKafkaRecord(message);

        // Handle partitioning based on message key
        if (message.getMessageKey() != null) {
            record = new ProducerRecord<>(
                message.getTopic(),
                calculatePartition(message.getMessageKey(), message.getTopic()),
                message.getMessageKey(),
                message.getPayload()
            );
        }

        // Add headers
        message.getHeaders().forEach((key, value) ->
            record.headers().add(key, value.getBytes()));

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                handleSendFailure(message, exception);
            } else {
                handleSendSuccess(message, metadata);
            }
        });
    }

    @Override
    public void subscribe(String topic, MessageHandler handler) {
        KafkaConsumer<String, byte[]> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList(topic));

        // Start consumer thread
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    UniversalMessage message = messageConverter.fromKafkaRecord(record);
                    try {
                        handler.handle(message);
                        // Manual commit for exactly-once processing
                        consumer.commitSync(Collections.singletonMap(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                        ));
                    } catch (Exception e) {
                        handleProcessingFailure(message, e);
                    }
                }
            }
        });

        consumers.put(topic, consumer);
    }

    @Override
    protected boolean supportsPartitioning() { return true; }

    @Override
    protected boolean supportsTransactions() { return true; }

    @Override
    protected boolean supportsMessageRetention() { return true; }

    @Override
    protected boolean supportsBroadcast() { return false; }

    // Kafka-specific optimization
    public void sendBatch(List<UniversalMessage> messages) {
        messages.forEach(this::send);
        producer.flush(); // Ensure all messages are sent
    }

    private int calculatePartition(String key, String topic) {
        TopicMetadata metadata = getTopicMetadata(topic);
        return Math.abs(key.hashCode()) % metadata.getPartitionCount();
    }
}
