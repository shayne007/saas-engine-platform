package com.feng.engine.messaging.config;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/8/11
 */
@Component
@Slf4j
public class DynamicMQConfigManager {
    private final ConfigurableEnvironment environment;
    private final Map<String, MessageQueueProvider> providers;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleConfigurationChange(ConfigurationChangeEvent event) {
        if (event.getKey().startsWith("universal-mq")) {
            String providerName = getCurrentProvider();
            MessageQueueProvider provider = providers.get(providerName);

            if (provider instanceof ReconfigurableProvider) {
                ReconfigurableProvider reconfigurable = (ReconfigurableProvider) provider;

                try {
                    Properties newConfig = buildConfiguration();
                    reconfigurable.updateConfiguration(newConfig);

                    log.info("Successfully updated configuration for provider: {}", providerName);

                } catch (Exception e) {
                    log.error("Failed to update configuration for provider: {}", providerName, e);
                    eventPublisher.publishEvent(new ConfigurationUpdateFailedEvent(providerName, e));
                }
            }
        }
    }

    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void validateProviderHealth() {
        String currentProvider = getCurrentProvider();
        MessageQueueProvider provider = providers.get(currentProvider);

        if (!provider.isHealthy()) {
            // Attempt to switch to backup provider
            switchToBackupProvider();
        }
    }

    private void switchToBackupProvider() {
        String backupProvider = getBackupProvider();
        if (backupProvider != null) {
            log.warn("Primary provider unhealthy, switching to backup: {}", backupProvider);

            // Update configuration
            System.setProperty("universal-mq.provider", backupProvider);

            // Trigger reconfiguration
            eventPublisher.publishEvent(new ProviderSwitchEvent(getCurrentProvider(), backupProvider));
        }
    }
}
