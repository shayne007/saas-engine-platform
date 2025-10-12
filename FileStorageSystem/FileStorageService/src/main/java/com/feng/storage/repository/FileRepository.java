package com.feng.storage.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.feng.storage.entity.FileEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

	Optional<FileEntity> findByFileHashAndProjectId(String fileHash, UUID projectId);

	List<FileEntity> findByProjectIdAndUploadStatus(UUID projectId,
			FileEntity.UploadStatus status);

	List<FileEntity> findByCreatedByAndUploadStatus(UUID createdBy,
			FileEntity.UploadStatus status);

	@Query(value = "SELECT f.*, " +
			"COUNT(*) OVER() as total_count " +
			"FROM files f " +
			"WHERE (:projectId IS NULL OR f.project_id = :projectId) " +
			"AND (:mimeTypes IS NULL OR f.mime_type = ANY(CAST(CAST(:mimeTypes AS text) " +
			"AS text[]))) " +
			"AND (:createdBy IS NULL OR f.created_by = :createdBy) " +
			"AND (:createdAfter IS NULL OR f.created_at >= :createdAfter) " +
			"AND (:createdBefore IS NULL OR f.created_at <= :createdBefore) " +
			"AND f.upload_status = 'COMPLETED' " +
			"ORDER BY f.created_at DESC", nativeQuery = true)
	Page<FileEntity> findFilesOptimized(
			@Param("projectId") UUID projectId,
			@Param("mimeTypes") String[] mimeTypes,
			@Param("createdBy") UUID createdBy,
			@Param("createdAfter") Instant createdAfter,
			@Param("createdBefore") Instant createdBefore,
			Pageable pageable
	);

	@Modifying
	@Query("DELETE FROM FileEntity f WHERE f.uploadStatus = 'FAILED' AND f.createdAt < " +
			":cutoffDate")
	int deleteFailedUploadsOlderThan(@Param("cutoffDate") Instant cutoffDate);

	@Modifying
	@Query("UPDATE FileEntity f " +
			"SET f.uploadStatus = 'EXPIRED' " +
			"WHERE f.expiresAt < :now AND f.uploadStatus = 'COMPLETED'")
	int markExpiredFiles(@Param("now") Instant now);

	@Query("SELECT COUNT(f) FROM FileEntity f WHERE f.projectId = :projectId AND f" +
			".uploadStatus = 'COMPLETED'")
	long countByProjectIdAndStatus(@Param("projectId") UUID projectId);

	@Query("SELECT SUM(f.fileSize) FROM FileEntity f WHERE f.projectId = :projectId AND " +
			"f.uploadStatus = 'COMPLETED'")
	Long getTotalSizeByProjectId(@Param("projectId") UUID projectId);
}
