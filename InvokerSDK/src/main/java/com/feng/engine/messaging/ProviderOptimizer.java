package com.feng.engine.messaging;

import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
public class ProviderOptimizer {

    public void optimizeForProvider(String providerName, ProducerConfig config) {
        switch (providerName) {
            case "kafka":
                optimizeKafkaProducer(config);
                break;
            case "redis":
                optimizeRedisProducer(config);
                break;
            case "rocketmq":
                optimizeRocketMQProducer(config);
                break;
        }
    }

    private void optimizeKafkaProducer(ProducerConfig config) {
        // Kafka-specific optimizations
        config.setBatchSize(65536);
        config.setLingerMs(10);
        config.setCompressionType("lz4");
        config.setMaxInFlightRequestsPerConnection(5);
        config.setEnableIdempotence(true);
    }

    private void optimizeRedisProducer(ProducerConfig config) {
        // Redis-specific optimizations
        config.setPipelining(true);
        config.setConnectionPoolSize(20);
        config.setMaxWaitTime(100);
        // Redis doesn't benefit from large batches like Kafka
        config.setBatchSize(10);
    }

    private void optimizeRocketMQProducer(ProducerConfig config) {
        // RocketMQ-specific optimizations
        config.setSendMsgTimeout(10000);
        config.setCompressMsgBodyOverHowmuch(4096);
        config.setRetryTimesWhenSendFailed(3);
        config.setMaxMessageSize(4194304); // 4MB
    }
}

