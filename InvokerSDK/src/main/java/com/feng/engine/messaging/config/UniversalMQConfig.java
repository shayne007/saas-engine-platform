package com.feng.engine.messaging.config;

import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@ConfigurationProperties(prefix = "universal-mq")
public class UniversalMQConfig {
    private String provider;
    private String brokers;
    private ProducerConfig producer;
    private ConsumerConfig consumer;
    private Map<String, Object> providerSpecific;

    public Properties toProviderProperties(String providerName) {
        Properties props = new Properties();

        switch (providerName) {
            case "kafka":
                return mapToKafkaProperties();
            case "redis":
                return mapToRedisProperties();
            case "rocketmq":
                return mapToRocketMQProperties();
            default:
                throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
    }

    private Properties mapToKafkaProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", ByteArraySerializer.class.getName());

        // Map universal producer config to Kafka-specific
        if (producer != null) {
            props.put("batch.size", producer.getBatchSize());
            props.put("linger.ms", producer.getLingerMs());
            props.put("retries", producer.getRetries());
            props.put("acks", producer.getAcknowledgment());
        }

        // Add provider-specific configurations
        if (providerSpecific != null && providerSpecific.containsKey("kafka")) {
            @SuppressWarnings("unchecked") Map<String, Object> kafkaConfig =
                (Map<String, Object>)providerSpecific.get("kafka");
            kafkaConfig.forEach((key, value) -> props.put(key, value));
        }

        return props;
    }

    private Properties mapToRedisProperties() {
        Properties props = new Properties();
        String[] hostPort = brokers.split(":");
        props.put("redis.host", hostPort[0]);
        props.put("redis.port", hostPort.length > 1 ? hostPort[1] : "6379");

        // Redis-specific mappings
        if (producer != null) {
            props.put("redis.pool.max-active", producer.getConnectionPoolSize());
            props.put("redis.timeout", producer.getTimeoutMs());
        }

        return props;
    }

    private Properties mapToRocketMQProperties() {
        Properties props = new Properties();
        props.put("rocketmq.name-server", brokers);

        // RocketMQ-specific mappings
        if (producer != null) {
            props.put("rocketmq.producer.send-msg-timeout", producer.getTimeoutMs());
            props.put("rocketmq.producer.retry-times", producer.getRetries());
            props.put("rocketmq.producer.max-message-size", producer.getMaxMessageSize());
        }

        return props;
    }
}

