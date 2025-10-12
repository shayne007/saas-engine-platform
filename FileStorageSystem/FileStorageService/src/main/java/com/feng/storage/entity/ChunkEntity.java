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
import javax.persistence.PrePersist;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
