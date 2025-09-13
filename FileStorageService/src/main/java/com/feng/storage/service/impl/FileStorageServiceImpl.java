package com.feng.storage.service.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.feng.storage.client.GcsStorageClient;
import com.feng.storage.entity.ChunkEntity;
import com.feng.storage.entity.ChunkedUploadEntity;
import com.feng.storage.entity.FileAccessLogEntity;
import com.feng.storage.entity.FileEntity;
import com.feng.storage.repository.ChunkRepository;
import com.feng.storage.repository.ChunkedUploadRepository;
import com.feng.storage.repository.FileAccessLogRepository;
import com.feng.storage.repository.FileRepository;
import com.feng.storage.service.api.ChunkInfo;
import com.feng.storage.service.api.ChunkUploadRequest;
import com.feng.storage.service.api.ChunkUploadResponse;
import com.feng.storage.service.api.ChunkedUploadRequest;
import com.feng.storage.service.api.ChunkedUploadResponse;
import com.feng.storage.service.api.CompleteChunkedUploadRequest;
import com.feng.storage.service.api.FileDownloadResponse;
import com.feng.storage.service.api.FileMetadata;
import com.feng.storage.service.api.FileQueryRequest;
import com.feng.storage.service.api.FileQueryResponse;
import com.feng.storage.service.api.FileStorageService;
import com.feng.storage.service.api.FileUploadRequest;
import com.feng.storage.service.api.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

	@Value("${file.storage.max-single-upload-size:10485760}") // 10MB default
	private long maxSingleUploadSize;
	
	@Value("${file.storage.default-chunk-size:5242880}") // 5MB default
	private int defaultChunkSize;

	private final FileRepository fileRepository;

	private final ChunkedUploadRepository chunkedUploadRepository;

	private final ChunkRepository chunkRepository;

	private final FileAccessLogRepository fileAccessLogRepository;

	private final GcsStorageClient gcsClient;

	@Override
	public FileUploadResponse uploadFile(MultipartFile file, FileUploadRequest request) {
		log.info("Processing file upload request for file: {} with MultipartFile",
				request.getOriginalFilename());

		validateUploadRequest(request);
		
		try {
			// Upload file content to temporary storage if needed
			// This could be implemented to store the file content temporarily before uploading to GCS
			// For now, we'll proceed with the same flow as the original method
			
			// Check for file deduplication
			Optional<FileEntity> existingFile = fileRepository
				.findByFileHashAndProjectId(request.getFileHash(),
						request.getProjectId() != null ?
								UUID.fromString(request.getProjectId()) : null);

			if (existingFile.isPresent() && request.getAllowDeduplication()) {
				log.info("File deduplication: returning existing file {}",
						existingFile.get().getId());
				return createDeduplicationResponse(existingFile.get());
			}

			// Create file entity
			FileEntity fileEntity = createFileEntity(request);
			fileEntity = fileRepository.save(fileEntity);

			// Upload file directly to GCS
			gcsClient.uploadFile(
					fileEntity.getGcsBucket(),
					fileEntity.getGcsObjectKey(),
					file.getInputStream(),
					file.getContentType()
			);

			// Update file status
			fileEntity.setUploadStatus(FileEntity.UploadStatus.COMPLETED);
			fileRepository.save(fileEntity);

			// Log access
			logFileAccess(fileEntity.getId(), request.getUserId(),
					FileAccessLogEntity.AccessType.UPLOAD);

			return FileUploadResponse.builder()
					.fileId(fileEntity.getId().toString())
					.isDuplicate(false)
					.build();

		} catch (IOException e) {
			log.error("Failed to process MultipartFile upload", e);
			throw new RuntimeException("Failed to process file upload", e);
		} catch (Exception e) {
			log.error("Failed to upload file", e);
			throw new RuntimeException("Failed to upload file", e);
		}
	}

	@Override
	public FileUploadResponse uploadFile(FileUploadRequest request) {
		log.info("Processing file upload request for file: {}",
				request.getOriginalFilename());

		validateUploadRequest(request);

		// Check for file deduplication
		Optional<FileEntity> existingFile = fileRepository
				.findByFileHashAndProjectId(request.getFileHash(),
						request.getProjectId() != null ?
								UUID.fromString(request.getProjectId()) : null);

		if (existingFile.isPresent() && request.getAllowDeduplication()) {
			log.info("File deduplication: returning existing file {}",
					existingFile.get().getId());
			return createDeduplicationResponse(existingFile.get());
		}

		// Create file entity
		FileEntity fileEntity = createFileEntity(request);
		fileEntity = fileRepository.save(fileEntity);

		try {
			// Generate signed upload URL
			String signedUrl = gcsClient.generateSignedUploadUrl(
					fileEntity.getGcsBucket(),
					fileEntity.getGcsObjectKey(),
					request.getMimeType(),
					Duration.ofHours(1)
			);

			// Log access
			logFileAccess(fileEntity.getId(), request.getUserId(),
					FileAccessLogEntity.AccessType.UPLOAD);

			return FileUploadResponse.builder()
					.fileId(fileEntity.getId().toString())
					.uploadUrl(signedUrl)
					.expiresAt(Instant.now().plus(Duration.ofHours(1)))
					.isDuplicate(false)
					.build();

		}
		catch (Exception e) {
			log.error("Failed to generate upload URL for file {}", fileEntity.getId(),
					e);
			fileEntity.setUploadStatus(FileEntity.UploadStatus.FAILED);
			fileRepository.save(fileEntity);
			throw new RuntimeException("Failed to generate upload URL", e);
		}
	}

	@Override
	public long getMaxSingleUploadSize() {
		return maxSingleUploadSize;
	}

	@Override
	public int getDefaultChunkSize() {
		return defaultChunkSize;
	}

	@Override
	public ChunkedUploadResponse initiateChunkedUpload(ChunkedUploadRequest request) {
		log.info("Processing chunked upload request for file: {}",
				request.getOriginalFilename());

		validateChunkedUploadRequest(request);

		// Create file entity
		FileEntity fileEntity = createFileEntityFromChunkedRequest(request);
		fileEntity = fileRepository.save(fileEntity);

		try {
			// Initiate multipart upload in GCS
			String uploadId = gcsClient.initiateMultipartUpload(
					fileEntity.getGcsBucket(),
					fileEntity.getGcsObjectKey(),
					request.getMimeType()
			);

			// Create chunked upload entity
			ChunkedUploadEntity chunkedUpload = ChunkedUploadEntity.builder()
					.fileId(fileEntity.getId())
					.uploadId(uploadId)
					.totalChunks(request.getTotalChunks())
					.chunkSize(request.getChunkSize())
					.expiresAt(Instant.now().plus(Duration.ofDays(1)))
					.build();

			chunkedUpload = chunkedUploadRepository.save(chunkedUpload);

			// Log access
			logFileAccess(fileEntity.getId(), request.getUserId(),
					FileAccessLogEntity.AccessType.UPLOAD);

			return ChunkedUploadResponse.builder()
					.fileId(fileEntity.getId().toString())
					.uploadId(chunkedUpload.getId().toString())
					.expiresAt(chunkedUpload.getExpiresAt())
					.build();

		}
		catch (Exception e) {
			log.error("Failed to initiate chunked upload for file {}",
					fileEntity.getId(),
					e);
			fileEntity.setUploadStatus(FileEntity.UploadStatus.FAILED);
			fileRepository.save(fileEntity);
			throw new RuntimeException("Failed to initiate chunked upload", e);
		}
	}

	@Override
	public ChunkUploadResponse uploadChunk(ChunkUploadRequest request) {
		log.debug("Processing chunk upload request for upload: {}, chunk: {}",
				request.getUploadId(), request.getChunkNumber());

		ChunkedUploadEntity chunkedUpload = chunkedUploadRepository
				.findById(UUID.fromString(request.getUploadId()))
				.orElseThrow(() -> new IllegalArgumentException("Upload not found"));

		validateChunkUploadRequest(request, chunkedUpload);

		try {
			// Generate signed URL for chunk upload
			String signedUrl = gcsClient.generateSignedChunkUploadUrl(
					chunkedUpload.getUploadId(),
					request.getChunkNumber(),
					Duration.ofHours(1)
			);

			return ChunkUploadResponse.builder()
					.chunkUploadUrl(signedUrl)
					.expiresAt(Instant.now().plus(Duration.ofHours(1)))
					.build();

		}
		catch (Exception e) {
			log.error("Failed to generate chunk upload URL for upload {} chunk {}",
					request.getUploadId(), request.getChunkNumber(), e);
			throw new RuntimeException("Failed to generate chunk upload URL", e);
		}
	}

	@Override
	public FileUploadResponse completeChunkedUpload(
			CompleteChunkedUploadRequest request) {
		log.info("Completing chunked upload: {}", request.getUploadId());

		ChunkedUploadEntity chunkedUpload = chunkedUploadRepository
				.findById(UUID.fromString(request.getUploadId()))
				.orElseThrow(() -> new IllegalArgumentException("Upload not found"));

		FileEntity fileEntity = fileRepository.findById(chunkedUpload.getFileId())
				.orElseThrow(() -> new IllegalArgumentException("File not found"));

		try {
			// Complete multipart upload in GCS
			String finalObjectKey = gcsClient.completeMultipartUpload(
					chunkedUpload.getUploadId(),
					fileEntity.getGcsObjectKey()
			);

			// Update file status
			fileEntity.setUploadStatus(FileEntity.UploadStatus.COMPLETED);
			fileEntity.setUpdatedAt(Instant.now());
			fileRepository.save(fileEntity);

			// Update chunked upload
			chunkedUpload.setCompletedChunks(request.getChunks().size());
			chunkedUploadRepository.save(chunkedUpload);

			// Save chunk information
			for (ChunkInfo chunkInfo : request.getChunks()) {
				ChunkEntity chunk = ChunkEntity.builder()
						.chunkedUploadId(chunkedUpload.getId())
						.chunkNumber(chunkInfo.getChunkNumber())
						.chunkSize(chunkedUpload.getChunkSize())
						.etag(chunkInfo.getEtag())
						.uploadedAt(Instant.now())
						.build();
				chunkRepository.save(chunk);
			}

			return FileUploadResponse.builder()
					.fileId(fileEntity.getId().toString())
					.uploadUrl(null)
					.expiresAt(Instant.now().plus(Duration.ofHours(1)))
					.isDuplicate(false)
					.build();

		}
		catch (Exception e) {
			log.error("Failed to complete chunked upload {}", request.getUploadId(), e);
			fileEntity.setUploadStatus(FileEntity.UploadStatus.FAILED);
			fileRepository.save(fileEntity);
			throw new RuntimeException("Failed to complete chunked upload", e);
		}
	}

	@Override
	public FileDownloadResponse getDownloadUrl(String fileId, Duration expiration) {
		log.info("Generating download URL for file: {}", fileId);

		FileEntity fileEntity = fileRepository.findById(UUID.fromString(fileId))
				.orElseThrow(() -> new IllegalArgumentException("File not found"));

		if (fileEntity.getUploadStatus() != FileEntity.UploadStatus.COMPLETED) {
			throw new IllegalStateException("File is not available for download");
		}

		try {
			String signedUrl = gcsClient.generateSignedDownloadUrl(
					fileEntity.getGcsBucket(),
					fileEntity.getGcsObjectKey(),
					expiration
			);

			// Log access
			logFileAccess(fileEntity.getId(), "system",
					FileAccessLogEntity.AccessType.DOWNLOAD);

			return FileDownloadResponse.builder()
					.downloadUrl(signedUrl)
					.filename(fileEntity.getOriginalFilename())
					.fileSize(fileEntity.getFileSize())
					.mimeType(fileEntity.getMimeType())
					.expiresAt(Instant.now().plus(expiration))
					.build();

		}
		catch (Exception e) {
			log.error("Failed to generate download URL for file {}", fileId, e);
			throw new RuntimeException("Failed to generate download URL", e);
		}
	}

	@Override
	public FileQueryResponse queryFiles(FileQueryRequest request) {
		log.debug("Querying files with filters: projectId={}, createdBy={}, " +
						"mimeTypes={}",
				request.getProjectId(), request.getCreatedBy(), request.getMimeTypes());

		// Build pageable
		Sort sort = Sort.by(Sort.Direction.DESC, "created_at");
		Pageable pageable = PageRequest.of(
				Optional.ofNullable(request.getPage()).orElse(0),
				Optional.ofNullable(request.getSize()).orElse(20),
				sort
		);

		// Convert request parameters
		UUID projectId =
				request.getProjectId() != null ?
						UUID.fromString(request.getProjectId()) :
						null;
		UUID createdBy =
				request.getCreatedBy() != null ?
						UUID.fromString(request.getCreatedBy()) :
						null;
		String[] mimeTypes = request.getMimeTypes() != null ?
				request.getMimeTypes().toArray(new String[0]) : null;

		// Query files
		Instant createdAfter =
				request.getCreatedAfter() != null ? request.getCreatedAfter() :
						Instant.EPOCH;
		Instant createdBefore =
				request.getCreatedBefore() != null ? request.getCreatedBefore() :
						Instant.now();
		Page<FileEntity> page = fileRepository.findFilesOptimized(
				projectId,
				mimeTypes,
				createdBy,
				createdAfter,
				createdBefore,
				pageable
		);

		// Convert to response
		List<FileMetadata> fileMetadataList = page.getContent().stream()
				.map(this::convertToFileMetadata)
				.collect(Collectors.toList());

		return FileQueryResponse.builder()
				.files(fileMetadataList)
				.total(page.getTotalElements())
				.build();
	}

	@Override
	public void deleteFile(String fileId, String userId) {
		log.info("Deleting file: {} by user: {}", fileId, userId);

		FileEntity fileEntity = fileRepository.findById(UUID.fromString(fileId))
				.orElseThrow(() -> new IllegalArgumentException("File not found"));

		try {
			// Delete from GCS
			boolean deleted = gcsClient.deleteBlob(fileEntity.getGcsBucket(),
					fileEntity.getGcsObjectKey());
			if (!deleted) {
				log.warn("Failed to delete file from GCS: {}/{}",
						fileEntity.getGcsBucket(), fileEntity.getGcsObjectKey());
			}

			// Delete from database
			fileRepository.delete(fileEntity);

			// Log access
			logFileAccess(fileEntity.getId(), userId,
					FileAccessLogEntity.AccessType.DELETE);

		}
		catch (Exception e) {
			log.error("Failed to delete file {}", fileId, e);
			throw new RuntimeException("Failed to delete file", e);
		}
	}

	@Override
	public FileMetadata getFileMetadata(String fileId) {
		log.debug("Getting file metadata for: {}", fileId);

		FileEntity fileEntity = fileRepository.findById(UUID.fromString(fileId))
				.orElseThrow(() -> new IllegalArgumentException("File not found"));

		// Log access
		logFileAccess(fileEntity.getId(), "system", FileAccessLogEntity.AccessType.VIEW);

		return convertToFileMetadata(fileEntity);
	}

	// Helper methods

	private void validateUploadRequest(FileUploadRequest request) {
		if (request.getOriginalFilename() == null ||
				request.getOriginalFilename().trim().isEmpty()) {
			throw new IllegalArgumentException("Original filename is required");
		}
		if (request.getFileSize() == null || request.getFileSize() <= 0) {
			throw new IllegalArgumentException("File size must be positive");
		}
		if (request.getMimeType() == null || request.getMimeType().trim().isEmpty()) {
			throw new IllegalArgumentException("MIME type is required");
		}
		if (request.getFileHash() == null || request.getFileHash().length() != 64) {
			throw new IllegalArgumentException(
					"File hash must be 64 characters (SHA-256)");
		}
		if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
			throw new IllegalArgumentException("User ID is required");
		}
	}

	private void validateChunkedUploadRequest(ChunkedUploadRequest request) {
		validateUploadRequest(FileUploadRequest.builder()
				.originalFilename(request.getOriginalFilename())
				.fileSize(request.getFileSize())
				.mimeType(request.getMimeType())
				.fileHash(request.getFileHash())
				.userId(request.getUserId())
				.projectId(request.getProjectId())
				.build());

		if (request.getTotalChunks() == null || request.getTotalChunks() < 2) {
			throw new IllegalArgumentException("Total chunks must be at least 2");
		}
		if (request.getChunkSize() == null || request.getChunkSize() < 1048576) { // 1MB
			throw new IllegalArgumentException("Chunk size must be at least 1MB");
		}
	}

	private void validateChunkUploadRequest(ChunkUploadRequest request,
			ChunkedUploadEntity chunkedUpload) {
		if (request.getChunkNumber() == null || request.getChunkNumber() < 1) {
			throw new IllegalArgumentException("Chunk number must be positive");
		}
		if (request.getChunkNumber() > chunkedUpload.getTotalChunks()) {
			throw new IllegalArgumentException("Chunk number exceeds total chunks");
		}
	}

	private FileEntity createFileEntity(FileUploadRequest request) {
		String filename = generateUniqueFilename(request.getOriginalFilename());
		String objectKey = generateObjectKey(request);

		return FileEntity.builder()
				.filename(filename)
				.originalFilename(request.getOriginalFilename())
				.fileSize(request.getFileSize())
				.mimeType(request.getMimeType())
				.fileHash(request.getFileHash())
				.gcsBucket(gcsClient.getDefaultBucket())
				.gcsObjectKey(objectKey)
				.uploadStatus(FileEntity.UploadStatus.PENDING)
				.createdBy(UUID.fromString(request.getUserId()))
				.projectId(request.getProjectId() != null ?
						UUID.fromString(request.getProjectId()) : null)
				.tags(request.getTags())
				.metadata(convertMetadataToString(request.getMetadata()))
				.build();
	}

	private FileEntity createFileEntityFromChunkedRequest(ChunkedUploadRequest request) {
		String filename = generateUniqueFilename(request.getOriginalFilename());
		String objectKey = generateObjectKeyFromChunkedRequest(request);

		return FileEntity.builder()
				.filename(filename)
				.originalFilename(request.getOriginalFilename())
				.fileSize(request.getFileSize())
				.mimeType(request.getMimeType())
				.fileHash(request.getFileHash())
				.gcsBucket(gcsClient.getDefaultBucket())
				.gcsObjectKey(objectKey)
				.uploadStatus(FileEntity.UploadStatus.UPLOADING)
				.createdBy(UUID.fromString(request.getUserId()))
				.projectId(request.getProjectId() != null ?
						UUID.fromString(request.getProjectId()) : null)
				.tags(request.getTags())
				.metadata(convertMetadataToString(request.getMetadata()))
				.build();
	}

	private String generateUniqueFilename(String originalFilename) {
		String timestamp = String.valueOf(Instant.now().toEpochMilli());
		String uuid = UUID.randomUUID().toString().substring(0, 8);
		return timestamp + "_" + uuid + "_" + originalFilename;
	}

	private String generateObjectKey(FileUploadRequest request) {
		String userId = request.getUserId();
		String projectId =
				request.getProjectId() != null ? request.getProjectId() : "default";
		String timestamp = Instant.now().toString().substring(0, 10); // YYYY-MM-DD
		return String.format("files/%s/%s/%s/%s", userId, projectId, timestamp,
				generateUniqueFilename(request.getOriginalFilename()));
	}

	private String generateObjectKeyFromChunkedRequest(ChunkedUploadRequest request) {
		String userId = request.getUserId();
		String projectId =
				request.getProjectId() != null ? request.getProjectId() : "default";
		String timestamp = Instant.now().toString().substring(0, 10); // YYYY-MM-DD
		return String.format("files/%s/%s/%s/%s", userId, projectId, timestamp,
				generateUniqueFilename(request.getOriginalFilename()));
	}

	private String convertMetadataToString(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return null;
		}
		// In a real implementation, you'd use Jackson to convert to JSON
		return metadata.toString();
	}

	private FileUploadResponse createDeduplicationResponse(FileEntity existingFile) {
		return FileUploadResponse.builder()
				.fileId(existingFile.getId().toString())
				.uploadUrl(null)
				.expiresAt(Instant.now().plus(Duration.ofHours(1)))
				.isDuplicate(true)
				.build();
	}

	private FileMetadata convertToFileMetadata(FileEntity fileEntity) {
		FileMetadata metadata = FileMetadata.builder()
				.id(fileEntity.getId().toString())
				.filename(fileEntity.getFilename())
				.originalFilename(fileEntity.getOriginalFilename())
				.fileSize(fileEntity.getFileSize())
				.mimeType(fileEntity.getMimeType())
				.fileHash(fileEntity.getFileHash())
				.gcsBucket(fileEntity.getGcsBucket())
				.gcsObjectKey(fileEntity.getGcsObjectKey())
				.uploadStatus(fileEntity.getUploadStatus().name())
				.createdBy(fileEntity.getCreatedBy().toString())
				.projectId(fileEntity.getProjectId() != null ?
						fileEntity.getProjectId().toString() : null)
				.tags(fileEntity.getTags())
				.metadata(parseMetadataFromString(fileEntity.getMetadata()))
				.createdAt(fileEntity.getCreatedAt())
				.updatedAt(fileEntity.getUpdatedAt())
				.expiresAt(fileEntity.getExpiresAt())
				.build();
		return metadata;
	}

	private Map<String, Object> parseMetadataFromString(String metadataString) {
		if (metadataString == null || metadataString.trim().isEmpty()) {
			return new HashMap<>();
		}
		// In a real implementation, you'd use Jackson to parse JSON
		return new HashMap<>();
	}

	private void logFileAccess(UUID fileId, String userId,
			FileAccessLogEntity.AccessType accessType) {
		try {
			FileAccessLogEntity logEntry = FileAccessLogEntity.builder()
					.fileId(fileId)
					.accessType(accessType)
					.userId(UUID.fromString(userId))
					.accessedAt(Instant.now())
					.build();
			fileAccessLogRepository.save(logEntry);
		}
		catch (Exception e) {
			log.warn("Failed to log file access for file {} by user {}", fileId, userId,
					e);
		}
	}
}
