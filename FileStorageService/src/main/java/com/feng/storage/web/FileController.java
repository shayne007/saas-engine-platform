package com.feng.storage.web;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feng.storage.entity.FileEntity;
import com.feng.storage.exception.FileValidationException;
import com.feng.storage.service.ChunkManagerService;
import com.feng.storage.service.api.ChunkUploadResponse;
import com.feng.storage.service.api.ChunkUploadResult;
import com.feng.storage.service.api.ChunkedUploadRequest;
import com.feng.storage.service.api.ChunkedUploadResponse;
import com.feng.storage.service.api.ChunkedUploadSession;
import com.feng.storage.service.api.CompleteChunkedUploadRequest;
import com.feng.storage.service.api.FileDownloadResponse;
import com.feng.storage.service.api.FileMetadata;
import com.feng.storage.service.api.FileQueryRequest;
import com.feng.storage.service.api.FileQueryResponse;
import com.feng.storage.service.api.FileStorageService;
import com.feng.storage.service.api.FileUploadRequest;
import com.feng.storage.service.api.FileUploadResponse;
import com.feng.storage.service.validation.FileValidationService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/files")
@Validated
@RequiredArgsConstructor
@Slf4j
public class FileController {

	private final FileStorageService fileStorageService;
	private final FileValidationService fileValidationService;
	private final ChunkManagerService chunkManagerService;



	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileUploadResponse> uploadFile(
			@Valid @RequestPart("file") MultipartFile file,
			@RequestParam(value = "projectId", required = false) String projectId,
			@RequestParam(value = "metadata", required = false) String metadata) {
		
		try {
			// Validate the file
			fileValidationService.validateFile(file);
			
			// Determine upload strategy based on file size
			if (file.getSize() > fileStorageService.getMaxSingleUploadSize()) {
				return initiateChunkedUploadFromFile(file, projectId, metadata);
			} else {
				return uploadDirectly(file, projectId, metadata);
			}
		} catch (FileValidationException e) {
			log.error("File validation failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(
				FileUploadResponse.builder()
					.error(e.getMessage())
					.build()
			);
		}
	}
	
	private ResponseEntity<FileUploadResponse> uploadDirectly(MultipartFile file, String projectId, String metadata) {
		try {
			// Create FileUploadRequest from MultipartFile
			FileUploadRequest request = buildFileUploadRequest(file, projectId, metadata);
			
			// Process the upload
			return ResponseEntity.ok(fileStorageService.uploadFile(file, request));
		} catch (IOException e) {
			throw new RuntimeException("Failed to process file upload", e);
		}
	}
	
	private ResponseEntity<FileUploadResponse> initiateChunkedUploadFromFile(MultipartFile file, String projectId, String metadata) {
		try {
			// Create chunked upload request from file
			ChunkedUploadRequest request = ChunkedUploadRequest.builder()
				.originalFilename(file.getOriginalFilename())
				.fileSize(file.getSize())
				.mimeType(file.getContentType())
				.fileHash(calculateFileHash(file))
				.userId(getCurrentUserId())
				.projectId(projectId)
				.metadata(parseMetadata(metadata))
				.totalChunks(calculateTotalChunks(file.getSize()))
				.chunkSize(fileStorageService.getDefaultChunkSize())
				.build();
			
			// Initiate chunked upload through the ChunkManagerService
			ChunkedUploadResponse response =
					fileStorageService.initiateChunkedUpload(request);
			
			// Return response with chunk upload details
			return ResponseEntity.status(HttpStatus.CREATED).body(FileUploadResponse.builder()
				.fileId(response.getFileId())
				.uploadId(response.getUploadId())
				.expiresAt(response.getExpiresAt())
				.chunkedUpload(true)
				.totalChunks(response.getTotalChunks())
				.chunkSize(response.getChunkSize())
				.build());
		} catch (IOException e) {
			throw new RuntimeException("Failed to initiate chunked upload", e);
		}
	}
	
	private FileUploadRequest buildFileUploadRequest(MultipartFile file, String projectId, String metadata) throws IOException {
		return FileUploadRequest.builder()
			.originalFilename(file.getOriginalFilename())
			.fileSize(file.getSize())
			.mimeType(file.getContentType())
			.fileHash(calculateFileHash(file))
			.userId(getCurrentUserId())
			.projectId(projectId)
			.metadata(parseMetadata(metadata))
			.build();
	}
	
	private String calculateFileHash(MultipartFile file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(file.getBytes());
			return bytesToHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to calculate file hash", e);
		}
	}
	
	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
	
	private int calculateTotalChunks(long fileSize) {
		return (int) Math.ceil((double) fileSize / fileStorageService.getDefaultChunkSize());
	}
	
	private String getCurrentUserId() {
		// In a real implementation, this would get the user ID from the security context
		return "2bf25b60-1b62-4230-85f2-a7c9d10d938b"; // Placeholder
	}
	
	private Map<String, Object> parseMetadata(String metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return new HashMap<>();
		}
		try {
			// Parse JSON metadata string to Map
			return new ObjectMapper().readValue(metadata, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid metadata format. Expected JSON", e);
		}
	}

	@PostMapping("/upload/chunked")
	public ResponseEntity<ChunkedUploadResponse> initiateChunkedUpload(
			@Valid @RequestBody ChunkedUploadRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(fileStorageService.initiateChunkedUpload(request));
	}
	
	@PostMapping("/upload/chunked/initiate")
	public ResponseEntity<ChunkedUploadResponse> initiateChunkedUploadDirect(
			@RequestParam("fileName") String fileName,
			@RequestParam("totalSize") long totalSize,
			@RequestParam(value = "chunkSize", required = false) Integer chunkSize,
			@RequestParam(value = "projectId", required = false) String projectId) {
		
		// Generate a unique upload ID
		String uploadId = UUID.randomUUID().toString();
		
		// Use default chunk size if not provided
		int actualChunkSize = chunkSize != null ? chunkSize : chunkManagerService.getDefaultChunkSize();
		
		// Create upload session
		ChunkedUploadSession session = chunkManagerService.createUploadSession(
			uploadId, fileName, totalSize, actualChunkSize, projectId);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(ChunkedUploadResponse.builder()
			.fileId(session.getFileId())
			.uploadId(uploadId)
			.totalChunks(session.getTotalChunks())
			.chunkSize(session.getChunkSize())
			.expiresAt(session.getExpiresAt())
			.build());
	}

	@PostMapping("/upload/chunked/{uploadId}/chunks/{chunkNumber}")
	public ResponseEntity<ChunkUploadResponse> getChunkUploadUrl(
			@PathVariable String uploadId, @PathVariable Integer chunkNumber) {
		// Get the upload session
		ChunkedUploadSession session = chunkManagerService.getUploadSession(uploadId);
		
		// Generate signed URL for chunk upload
		String uploadUrl = chunkManagerService.generateChunkUploadUrl(uploadId, chunkNumber);
		
		return ResponseEntity.ok(ChunkUploadResponse.builder()
				.chunkUploadUrl(uploadUrl)
				.expiresAt(session.getExpiresAt())
				.build());
	}
	
	// This method is removed as it's a duplicate of the one below
	// The second implementation at line @PutMapping("/upload/chunked/{uploadId}/chunks/{chunkNumber}") is kept

	@PostMapping("/upload/chunked/{uploadId}/complete")
	public ResponseEntity<FileUploadResponse> completeChunkedUpload(
			@PathVariable String uploadId,
			@Valid @RequestBody CompleteChunkedUploadRequest request) {
		request.setUploadId(uploadId);
		return ResponseEntity.ok(fileStorageService.completeChunkedUpload(request));
	}
	
	@PutMapping("/upload/chunked/{uploadId}/chunks/{chunkNumber}")
	public ResponseEntity<ChunkUploadResponse> uploadChunk(
			@PathVariable String uploadId,
			@PathVariable Integer chunkNumber,
			@RequestParam("chunk") MultipartFile chunk) {
		
		try {
			// Upload the chunk
			ChunkUploadResult result = chunkManagerService.uploadChunk(uploadId, chunkNumber, chunk);
			
			// Check if all chunks are uploaded
			if (result.isComplete()) {
				// Finalize the upload
				FileEntity fileEntity = chunkManagerService.finalizeUpload(uploadId);
				
				// Return response with completed flag and fileId
				return ResponseEntity.ok(ChunkUploadResponse.builder()
						.completed(true)
						.fileId(fileEntity.getId().toString())
						.build());
			}
			
			// Return response with completed flag set to false
			return ResponseEntity.ok(ChunkUploadResponse.builder()
					.completed(false)
					.build());
		} catch (Exception e) {
			log.error("Error uploading chunk: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ChunkUploadResponse.builder().build());
		}
	}

	@GetMapping("/{fileId}/download")
	public ResponseEntity<FileDownloadResponse> getDownloadUrl(
			@PathVariable String fileId,
			@RequestParam(defaultValue = "3600") Long expirationSeconds) {
		Duration expiration = Duration.ofSeconds(expirationSeconds);
		return ResponseEntity.ok(fileStorageService.getDownloadUrl(fileId, expiration));
	}

	@GetMapping
	public ResponseEntity<FileQueryResponse> queryFiles(
			@RequestParam(required = false) String projectId,
			@RequestParam(required = false) List<String> mimeTypes,
			@RequestParam(required = false) String createdBy,
			@RequestParam(required = false) Instant createdAfter,
			@RequestParam(required = false) Instant createdBefore,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "20") Integer size,
			@RequestParam(defaultValue = "createdAt,desc") String sort) {

		FileQueryRequest req = FileQueryRequest.builder()
				.projectId(projectId)
				.mimeTypes(mimeTypes)
				.createdBy(createdBy)
				.createdAfter(createdAfter)
				.createdBefore(createdBefore)
				.page(page)
				.size(size)
				.sort(sort)
				.build();
		return ResponseEntity.ok(fileStorageService.queryFiles(req));
	}

	@GetMapping("/{fileId}")
	public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable String fileId) {
		return ResponseEntity.ok(fileStorageService.getFileMetadata(fileId));
	}

	@DeleteMapping("/{fileId}")
	public ResponseEntity<Void> deleteFile(@PathVariable String fileId,
			@RequestHeader(value = "X-User-Id", required = false) String userId) {
		fileStorageService.deleteFile(fileId, userId == null ? "system" : userId);
		return ResponseEntity.noContent().build();
	}
}


