package com.feng.engine.messaging.config;

import lombok.Data;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Data
public class ProducerConfig {
    private int batchSize;
    private int lingerMs;
    private int retries;
    private int acknowledgment;
    private String compressionType;
    private int maxInFlightRequestsPerConnection;
    private boolean enableIdempotence;
    private boolean pipelining;
    private int connectionPoolSize;
    private int maxWaitTime;
    private int timeoutMs;
    private int compressMsgBodyOverHowmuch;
    private int retryTimesWhenSendFailed;
    private int maxMessageSize;

}
