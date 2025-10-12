package com.feng.storage.config;

import java.io.IOException;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@Slf4j
public class GcsConfig {

	@Value("${gcs.project.id:}")
	private String projectId;

	@Value("${gcs.credentials.path:}")
	private String credentialsPath;

	@Bean
	public Storage storage() {
		try {
			StorageOptions.Builder builder = StorageOptions.newBuilder();

			if (projectId != null && !projectId.isEmpty()) {
				builder.setProjectId(projectId);
			}

			if (credentialsPath != null && !credentialsPath.isEmpty()) {
				try {
					builder.setCredentials(ServiceAccountCredentials.fromStream(
							new ClassPathResource(credentialsPath).getInputStream()));
				}
				catch (IOException e) {
					throw new RuntimeException(
							"Failed to load GCS credentials from path: " +
									credentialsPath, e);
				}
			}

			Storage storage = builder.build().getService();
			log.info("GCS Storage client initialized successfully");
			return storage;
		}
		catch (Exception e) {
			log.error("Failed to initialize GCS Storage client", e);
			throw new RuntimeException("Failed to initialize GCS client", e);
		}
	}
}
