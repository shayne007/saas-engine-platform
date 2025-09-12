package com.feng.storage.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "chunked_upload_id", nullable = false)
    private UUID chunkedUploadId;
    
    @Column(name = "chunk_number", nullable = false)
    private Integer chunkNumber;
    
    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;
    
    @Column(name = "etag", length = 255)
    private String etag;
    
    @Column(name = "uploaded_at")
    private Instant uploadedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunked_upload_id", insertable = false, updatable = false)
    private ChunkedUploadEntity chunkedUpload;
    
    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
