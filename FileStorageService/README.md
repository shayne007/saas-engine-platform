# File Storage Service

A production-ready file storage service built with Spring Boot, PostgreSQL, and Google Cloud Storage (GCS). This service provides secure file upload, download, and management capabilities with support for chunked uploads for large files.

## Features

- **File Upload & Download**: Support for single file uploads and chunked uploads for large files
- **Google Cloud Storage Integration**: Secure file storage using GCS with signed URLs
- **PostgreSQL Database**: Metadata storage with full ACID compliance
- **File Deduplication**: SHA-256 based deduplication to save storage space
- **Security**: Input validation, file type restrictions, and access logging
- **Monitoring**: Health checks, metrics, and comprehensive logging
- **Cleanup**: Automated cleanup of failed uploads and old access logs
- **Docker Support**: Complete containerization with Docker Compose

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Client App    │───▶│  File Storage    │───▶│   PostgreSQL    │
│                 │    │     Service      │    │    Database     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │ Google Cloud     │
                       │    Storage       │
                       └──────────────────┘
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- Google Cloud Platform account with GCS enabled

### 1. Clone and Build

```bash
git clone <repository-url>
cd FileStorageService
mvn clean package
```

### 2. Set up Google Cloud Storage

1. Create a GCS bucket for file storage
2. Create a service account with Storage Admin permissions
3. Download the service account JSON key file
4. Set environment variables:

```bash
export GCS_PROJECT_ID=your-project-id
export GCS_BUCKET_NAME=your-bucket-name
export GCS_CREDENTIALS_PATH=/path/to/service-account.json
```

### 3. Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# Check service health
curl http://localhost:8080/api/v1/actuator/health
```

### 4. Run Locally (Development)

```bash
# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

## API Endpoints

### File Upload

**Single File Upload**
```bash
POST /api/v1/files/upload
Content-Type: application/json

{
  "originalFilename": "document.pdf",
  "fileSize": 1048576,
  "mimeType": "application/pdf",
  "fileHash": "sha256hash...",
  "userId": "user-123",
  "projectId": "project-456",
  "allowDeduplication": true
}
```

**Chunked Upload (Large Files)**
```bash
# 1. Initiate chunked upload
POST /api/v1/files/upload/chunked
{
  "originalFilename": "large-file.zip",
  "fileSize": 52428800,
  "mimeType": "application/zip",
  "fileHash": "sha256hash...",
  "userId": "user-123",
  "totalChunks": 10,
  "chunkSize": 5242880
}

# 2. Upload each chunk
POST /api/v1/files/upload/chunked/{uploadId}/chunks/{chunkNumber}

# 3. Complete upload
POST /api/v1/files/upload/chunked/{uploadId}/complete
{
  "chunks": [
    {"chunkNumber": 1, "etag": "etag1"},
    {"chunkNumber": 2, "etag": "etag2"}
  ]
}
```

### File Download

```bash
GET /api/v1/files/{fileId}/download?expirationSeconds=3600
```

### File Management

```bash
# Query files
GET /api/v1/files?projectId=project-456&page=0&size=20

# Get file metadata
GET /api/v1/files/{fileId}

# Delete file
DELETE /api/v1/files/{fileId}
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `file_storage` |
| `DB_USERNAME` | Database username | `file_storage_user` |
| `DB_PASSWORD` | Database password | `file_storage_password` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `GCS_PROJECT_ID` | GCP Project ID | Required |
| `GCS_BUCKET_NAME` | GCS Bucket name | Required |
| `GCS_CREDENTIALS_PATH` | Service account JSON path | Required |

### Application Properties

Key configuration options in `application.yml`:

```yaml
file-storage:
  max-file-size: 104857600 # 100MB
  chunk-size: 5242880 # 5MB
  upload-timeout: 3600 # 1 hour
  cleanup:
    enabled: true
    interval: 3600000 # 1 hour
    failed-upload-retention-days: 7
    access-log-retention-days: 90
```

## Database Schema

The service uses PostgreSQL with the following main tables:

- `files`: File metadata and status
- `chunked_uploads`: Chunked upload sessions
- `chunks`: Individual chunk information
- `file_access_logs`: Access audit trail
- `file_tags`: File tagging system

See `src/main/resources/db/migration/` for complete schema.

## Security

- **File Type Validation**: Only allowed MIME types are accepted
- **File Size Limits**: Configurable maximum file sizes
- **Input Validation**: Comprehensive request validation
- **Access Logging**: All file operations are logged
- **Signed URLs**: Secure file access without exposing credentials

## Monitoring

### Health Checks

```bash
# Application health
GET /api/v1/actuator/health

# Detailed health info
GET /api/v1/actuator/health/liveness
GET /api/v1/actuator/health/readiness
```

### Metrics

```bash
# Prometheus metrics
GET /api/v1/actuator/prometheus
```

Key metrics:
- `file_uploads_total`: Total file uploads
- `file_downloads_total`: Total file downloads
- `file_upload_duration`: Upload duration histogram
- `file_size_bytes`: File size distribution

## Development

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn test -Dtest=*IntegrationTest
```

### Code Quality

```bash
# Check code style
mvn checkstyle:check

# Run static analysis
mvn spotbugs:check
```

## Deployment

### Docker

```bash
# Build image
docker build -t file-storage-service .

# Run container
docker run -p 8080:8080 \
  -e GCS_PROJECT_ID=your-project \
  -e GCS_BUCKET_NAME=your-bucket \
  -e GCS_CREDENTIALS_PATH=/path/to/key.json \
  file-storage-service
```

### Kubernetes

See `k8s/` directory for Kubernetes deployment manifests.

## Troubleshooting

### Common Issues

1. **GCS Authentication**: Ensure service account has proper permissions
2. **Database Connection**: Check PostgreSQL is running and accessible
3. **File Size Limits**: Adjust `file-storage.max-file-size` if needed
4. **Memory Issues**: Increase JVM heap size for large files

### Logs

```bash
# View application logs
docker-compose logs -f file-storage-service

# View specific log level
docker-compose logs -f file-storage-service | grep ERROR
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
