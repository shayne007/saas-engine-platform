package com.feng.calendar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used for external API calls
 */
@Configuration
public class WebClientConfig {

	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder()
				.codecs(configurer -> configurer.defaultCodecs()
						.maxInMemorySize(1024 * 1024)) // 1MB
				.build()
				.mutate();
	}

	@Bean
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder.build();
	}
}
