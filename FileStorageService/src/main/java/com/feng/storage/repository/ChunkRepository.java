package com.feng.storage.repository;

import com.feng.storage.entity.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {
    
    Optional<ChunkEntity> findByChunkedUploadIdAndChunkNumber(UUID chunkedUploadId, Integer chunkNumber);
    
    List<ChunkEntity> findByChunkedUploadIdOrderByChunkNumber(UUID chunkedUploadId);
    
    @Query("SELECT COUNT(c) FROM ChunkEntity c WHERE c.chunkedUploadId = :chunkedUploadId AND c.etag IS NOT NULL")
    long countCompletedChunksByUploadId(@Param("chunkedUploadId") UUID chunkedUploadId);
    
    @Modifying
    @Query("DELETE FROM ChunkEntity c WHERE c.chunkedUploadId IN (SELECT cu.id FROM ChunkedUploadEntity cu WHERE cu.expiresAt < :now)")
    int deleteChunksForExpiredUploads(@Param("now") Instant now);
    
    @Query("SELECT c FROM ChunkEntity c WHERE c.chunkedUploadId = :chunkedUploadId AND c.etag IS NULL")
    List<ChunkEntity> findIncompleteChunksByUploadId(@Param("chunkedUploadId") UUID chunkedUploadId);
}
