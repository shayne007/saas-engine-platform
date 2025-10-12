package com.feng.storage.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.feng.storage.entity.ChunkedUploadEntity;
import com.feng.storage.entity.FileEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChunkedUploadRepository
		extends JpaRepository<ChunkedUploadEntity, UUID> {

	Optional<ChunkedUploadEntity> findByUploadId(String uploadId);

	List<ChunkedUploadEntity> findByFileId(UUID fileId);

	@Query("SELECT COUNT(c) FROM ChunkedUploadEntity c WHERE c.file.uploadStatus IN " +
			":statuses")
	long countByStatusIn(@Param("statuses") List<FileEntity.UploadStatus> statuses);

	@Modifying
	@Query("DELETE FROM ChunkedUploadEntity c WHERE c.expiresAt < :now")
	int deleteExpiredUploads(@Param("now") Instant now);

	@Query("SELECT c FROM ChunkedUploadEntity c WHERE c.fileId = :fileId AND c" +
			".completedChunks = c.totalChunks")
	Optional<ChunkedUploadEntity> findCompletedUploadByFileId(
			@Param("fileId") UUID fileId);
}
