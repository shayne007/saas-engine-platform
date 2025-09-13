package com.feng.storage.entity;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Table(name = "file_access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAccessLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	@Column(name = "file_id")
	private UUID fileId;

	@Enumerated(EnumType.STRING)
	@Column(name = "access_type", nullable = false, length = 20)
	private AccessType accessType;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "ip_address")
	private InetAddress ipAddress;

	@Column(name = "user_agent", columnDefinition = "TEXT")
	private String userAgent;

	@CreationTimestamp
	@Column(name = "accessed_at", nullable = false)
	private Instant accessedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id", insertable = false, updatable = false)
	private FileEntity file;


	public enum AccessType {
		UPLOAD, DOWNLOAD, DELETE, VIEW
	}
}
