package com.feng.storage;

import com.feng.storage.service.api.FileStorageService;
import com.feng.storage.service.api.FileUploadRequest;
import com.feng.storage.service.api.FileUploadResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class FileStorageServiceIntegrationTest {

	@Autowired
	private FileStorageService fileStorageService;

	@Test
	public void testFileUpload() {
		FileUploadRequest request = FileUploadRequest.builder()
				.originalFilename("test.pdf")
				.fileSize(1024L)
				.mimeType("application/pdf")
				.fileHash(
						"abcd1234567890abcd1234567890abcd1234567890abcd1234567890abcd1234")
				.userId("user-123")
				.projectId("project-456")
				.build();

		FileUploadResponse response = fileStorageService.uploadFile(request);

		assertNotNull(response);
		assertNotNull(response.getFileId());
		assertFalse(response.getIsDuplicate());
	}
}
