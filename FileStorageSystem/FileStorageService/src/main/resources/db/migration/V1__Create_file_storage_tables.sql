-- Create files table for storing file metadata
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_hash VARCHAR(64) NOT NULL, -- SHA-256 hash for deduplication
    gcs_bucket VARCHAR(100) NOT NULL,
    gcs_object_key VARCHAR(500) NOT NULL,
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, UPLOADING, COMPLETED, FAILED, EXPIRED
    created_by UUID NOT NULL,
    project_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE, -- For temporary files
    
    CONSTRAINT valid_upload_status CHECK (upload_status IN ('PENDING', 'UPLOADING', 'COMPLETED', 'FAILED', 'EXPIRED'))
);

-- Create file_tags table for storing file tags
CREATE TABLE file_tags (
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    tag_key VARCHAR(255) NOT NULL,
    tag_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (file_id, tag_key)
);

-- Create chunked_uploads table for managing large file uploads
CREATE TABLE chunked_uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    upload_id VARCHAR(255) NOT NULL, -- GCS multipart upload ID
    total_chunks INTEGER NOT NULL,
    completed_chunks INTEGER DEFAULT 0,
    chunk_size INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL -- Cleanup incomplete uploads
);

-- Create chunks table for tracking individual chunk uploads
CREATE TABLE chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunked_upload_id UUID NOT NULL REFERENCES chunked_uploads(id) ON DELETE CASCADE,
    chunk_number INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    etag VARCHAR(255), -- GCS ETag for the chunk
    uploaded_at TIMESTAMP WITH TIME ZONE,
    
    UNIQUE(chunked_upload_id, chunk_number)
);

-- Create file_access_logs table for audit and analytics
CREATE TABLE file_access_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID REFERENCES files(id) ON DELETE SET NULL,
    access_type VARCHAR(20) NOT NULL, -- UPLOAD, DOWNLOAD, DELETE, VIEW
    user_id UUID NOT NULL,
    ip_address BYTEA,
    user_agent TEXT,
    accessed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_files_project_id ON files(project_id);
CREATE INDEX idx_files_created_by ON files(created_by);
CREATE INDEX idx_files_file_hash ON files(file_hash);
CREATE INDEX idx_files_upload_status ON files(upload_status);
CREATE INDEX idx_files_created_at ON files(created_at DESC);
CREATE INDEX idx_files_gcs_bucket_object ON files(gcs_bucket, gcs_object_key);

CREATE INDEX idx_chunked_uploads_file_id ON chunked_uploads(file_id);
CREATE INDEX idx_chunked_uploads_upload_id ON chunked_uploads(upload_id);
CREATE INDEX idx_chunked_uploads_expires_at ON chunked_uploads(expires_at);

CREATE INDEX idx_chunks_chunked_upload_id ON chunks(chunked_upload_id);
CREATE INDEX idx_chunks_chunk_number ON chunks(chunk_number);

CREATE INDEX idx_file_access_logs_file_id ON file_access_logs(file_id);
CREATE INDEX idx_file_access_logs_user_id ON file_access_logs(user_id);
CREATE INDEX idx_file_access_logs_accessed_at ON file_access_logs(accessed_at DESC);
CREATE INDEX idx_file_access_logs_access_type ON file_access_logs(access_type);

-- Add metadata column to files table (JSONB for extensible metadata)
ALTER TABLE files ADD COLUMN metadata JSONB;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_files_updated_at 
    BEFORE UPDATE ON files 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
