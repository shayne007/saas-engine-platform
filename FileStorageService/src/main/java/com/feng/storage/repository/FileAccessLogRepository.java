package com.feng.storage.repository;

import com.feng.storage.entity.FileAccessLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FileAccessLogRepository extends JpaRepository<FileAccessLogEntity, UUID> {
    
    List<FileAccessLogEntity> findByFileIdOrderByAccessedAtDesc(UUID fileId);
    
    List<FileAccessLogEntity> findByUserIdOrderByAccessedAtDesc(UUID userId);
    
    @Query("SELECT l FROM FileAccessLogEntity l WHERE l.fileId = :fileId AND l.accessType = :accessType ORDER BY l.accessedAt DESC")
    List<FileAccessLogEntity> findByFileIdAndAccessType(@Param("fileId") UUID fileId, 
                                                       @Param("accessType") FileAccessLogEntity.AccessType accessType);
    
    @Query("SELECT COUNT(l) FROM FileAccessLogEntity l WHERE l.fileId = :fileId AND l.accessType = 'DOWNLOAD'")
    long countDownloadsByFileId(@Param("fileId") UUID fileId);
    
    @Query("SELECT l FROM FileAccessLogEntity l WHERE l.accessedAt BETWEEN :startTime AND :endTime ORDER BY l.accessedAt DESC")
    Page<FileAccessLogEntity> findByAccessedAtBetween(@Param("startTime") Instant startTime, 
                                                     @Param("endTime") Instant endTime, 
                                                     Pageable pageable);
    
    @Modifying
    @Query("DELETE FROM FileAccessLogEntity l WHERE l.accessedAt < :cutoffDate")
    int deleteOldLogs(@Param("cutoffDate") Instant cutoffDate);
}
