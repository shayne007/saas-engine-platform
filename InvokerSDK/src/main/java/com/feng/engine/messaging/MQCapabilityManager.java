package com.feng.engine.messaging;

import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
public class MQCapabilityManager {
    private final Map<String, ProviderCapabilities> capabilities;

    public ProviderCapabilities detectCapabilities(String providerName) {
        return capabilities.computeIfAbsent(providerName, this::discoverCapabilities);
    }

    private ProviderCapabilities discoverCapabilities(String providerName) {
        MessageQueueProvider provider = getProvider(providerName);

        return ProviderCapabilities.builder()
            .providerName(providerName)
            .supportsTransactions(testTransactionSupport(provider))
            .supportsPartitioning(testPartitioningSupport(provider))
            .supportsBroadcast(testBroadcastSupport(provider))
            .supportsMessageRetention(testRetentionSupport(provider))
            .supportsDeadLetterQueue(testDLQSupport(provider))
            .maxMessageSize(discoverMaxMessageSize(provider))
            .maxBatchSize(discoverMaxBatchSize(provider))
            .supportedCompressionTypes(discoverCompressionTypes(provider))
            .build();
    }

    public void adaptMessage(UniversalMessage message, ProviderCapabilities capabilities) {
        // Adapt message based on provider capabilities
        if (message.getPayload().length > capabilities.getMaxMessageSize()) {
            // Compress or split message
            compressMessage(message, capabilities.getSupportedCompressionTypes());
        }

        if (message.isBroadcast() && !capabilities.isSupportsBroadcast()) {
            // Simulate broadcast using multiple sends
            simulateBroadcast(message);
        }

        if (message.isTransactional() && !capabilities.isSupportsTransactions()) {
            // Use application-level transaction simulation
            simulateTransaction(message);
        }
    }
}

