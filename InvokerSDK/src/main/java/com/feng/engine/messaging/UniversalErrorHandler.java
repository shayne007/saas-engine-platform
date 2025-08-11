package com.feng.engine.messaging;

import java.util.concurrent.TimeoutException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
public class UniversalErrorHandler {

    public MessageQueueException handleProviderError(Exception providerError, String provider) {
        switch (provider) {
            case "kafka":
                return handleKafkaError(providerError);
            case "redis":
                return handleRedisError(providerError);
            case "rocketmq":
                return handleRocketMQError(providerError);
            default:
                return new MessageQueueException("Unknown provider error", providerError);
        }
    }

    private MessageQueueException handleKafkaError(Exception error) {
        if (error instanceof TimeoutException) {
            return new MessageTimeoutException("Kafka send timeout", error);
        } else if (error instanceof RecordTooLargeException) {
            return new MessageSizeException("Message too large for Kafka", error);
        } else if (error instanceof NotLeaderForPartitionException) {
            return new MessageRoutingException("Partition leader not available", error, true);
        } else if (error instanceof NetworkException) {
            return new MessageNetworkException("Kafka network error", error, true);
        }

        return new MessageQueueException("Kafka error", error);
    }

    private MessageQueueException handleRedisError(Exception error) {
        if (error instanceof JedisConnectionException) {
            return new MessageNetworkException("Redis connection failed", error, true);
        } else if (error instanceof JedisDataException) {
            if (error.getMessage().contains("NOGROUP")) {
                return new MessageConfigurationException("Redis consumer group not found", error);
            }
        }

        return new MessageQueueException("Redis error", error);
    }

    private MessageQueueException handleRocketMQError(Exception error) {
        if (error instanceof MQBrokerException) {
            MQBrokerException brokerError = (MQBrokerException) error;
            switch (brokerError.getResponseCode()) {
                case ResponseCode.TOPIC_NOT_EXIST:
                    return new MessageConfigurationException("RocketMQ topic not found", error);
                case ResponseCode.MESSAGE_ILLEGAL:
                    return new MessageValidationException("Invalid RocketMQ message", error);
                case ResponseCode.SYSTEM_BUSY:
                    return new MessageBusyException("RocketMQ system busy", error, true);
            }
        } else if (error instanceof MQClientException) {
            return new MessageConfigurationException("RocketMQ client error", error);
        }

        return new MessageQueueException("RocketMQ error", error);
    }
}

