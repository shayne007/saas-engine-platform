package com.feng.storage.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "files")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	@Column(name = "filename", nullable = false, length = 255)
	private String filename;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@Column(name = "mime_type", nullable = false, length = 100)
	private String mimeType;

	@Column(name = "file_hash", nullable = false, length = 64)
	private String fileHash;

	@Column(name = "gcs_bucket", nullable = false, length = 100)
	private String gcsBucket;

	@Column(name = "gcs_object_key", nullable = false, length = 500)
	private String gcsObjectKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "upload_status", nullable = false, length = 20)
	private UploadStatus uploadStatus;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "project_id")
	private UUID projectId;

	@ElementCollection
	@CollectionTable(name = "file_tags", joinColumns = @JoinColumn(name = "file_id"))
	@MapKeyColumn(name = "tag_key")
	@Column(name = "tag_value")
	private Map<String, String> tags;

	@Type(type = "jsonb")
	@Column(name = "metadata", columnDefinition = "jsonb")
	private String metadata; // JSON string for extensible metadata

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;


	public enum UploadStatus {
		PENDING, UPLOADING, COMPLETED, FAILED, EXPIRED
	}
}
