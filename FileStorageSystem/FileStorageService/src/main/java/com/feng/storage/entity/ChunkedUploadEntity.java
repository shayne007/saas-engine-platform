package com.feng.storage.entity;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "chunked_uploads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkedUploadEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	@Column(name = "file_id", nullable = false)
	private UUID fileId;

	@Column(name = "upload_id", nullable = false, length = 255)
	private String uploadId;

	@Column(name = "total_chunks", nullable = false)
	private Integer totalChunks;

	@Column(name = "completed_chunks", nullable = false)
	@Builder.Default
	private Integer completedChunks = 0;

	@Column(name = "chunk_size", nullable = false)
	private Integer chunkSize;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id", insertable = false, updatable = false)
	private FileEntity file;
}
