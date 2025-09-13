package com.feng.storage.service;

import java.time.Instant;

import com.feng.storage.repository.ChunkedUploadRepository;
import com.feng.storage.repository.FileAccessLogRepository;
import com.feng.storage.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

	private final FileRepository fileRepository;

	private final ChunkedUploadRepository chunkedUploadRepository;

	private final FileAccessLogRepository fileAccessLogRepository;

	@Value("${file-storage.cleanup.enabled:true}")
	private boolean cleanupEnabled;

	@Value("${file-storage.cleanup.failed-upload-retention-days:7}")
	private int failedUploadRetentionDays;

	@Value("${file-storage.cleanup.access-log-retention-days:90}")
	private int accessLogRetentionDays;

	@Scheduled(fixedRateString = "${file-storage.cleanup.interval:3600000}")
	@Transactional
	public void performCleanup() {
		if (!cleanupEnabled) {
			log.debug("Cleanup is disabled, skipping");
			return;
		}

		log.info("Starting cleanup process");

		try {
			// Clean up failed uploads
			cleanupFailedUploads();

			// Clean up expired chunked uploads
			cleanupExpiredChunkedUploads();

			// Mark expired files
			markExpiredFiles();

			// Clean up old access logs
			cleanupOldAccessLogs();

			log.info("Cleanup process completed successfully");
		}
		catch (Exception e) {
			log.error("Error during cleanup process", e);
		}
	}

	private void cleanupFailedUploads() {
		Instant cutoffDate =
				Instant.now().minusSeconds(failedUploadRetentionDays * 24 * 60 * 60);
		int deletedCount = fileRepository.deleteFailedUploadsOlderThan(cutoffDate);
		log.info("Cleaned up {} failed uploads older than {} days", deletedCount,
				failedUploadRetentionDays);
	}

	private void cleanupExpiredChunkedUploads() {
		int deletedCount = chunkedUploadRepository.deleteExpiredUploads(Instant.now());
		log.info("Cleaned up {} expired chunked uploads", deletedCount);
	}

	private void markExpiredFiles() {
		int updatedCount = fileRepository.markExpiredFiles(Instant.now());
		log.info("Marked {} files as expired", updatedCount);
	}

	private void cleanupOldAccessLogs() {
		Instant cutoffDate =
				Instant.now().minusSeconds(accessLogRetentionDays * 24 * 60 * 60);
		int deletedCount = fileAccessLogRepository.deleteOldLogs(cutoffDate);
		log.info("Cleaned up {} old access logs older than {} days", deletedCount,
				accessLogRetentionDays);
	}
}
