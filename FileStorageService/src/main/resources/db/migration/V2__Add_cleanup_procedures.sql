-- Create cleanup procedures for maintenance

-- Procedure to clean up expired chunked uploads
CREATE OR REPLACE FUNCTION cleanup_expired_uploads()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Delete expired chunked uploads and their chunks
    WITH expired_uploads AS (
        DELETE FROM chunked_uploads 
        WHERE expires_at < NOW() 
        RETURNING id
    )
    DELETE FROM chunks 
    WHERE chunked_upload_id IN (SELECT id FROM expired_uploads);
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Delete the expired uploads themselves
    DELETE FROM chunked_uploads WHERE expires_at < NOW();
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Procedure to clean up failed uploads older than specified days
CREATE OR REPLACE FUNCTION cleanup_failed_uploads(days_old INTEGER DEFAULT 7)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMP WITH TIME ZONE;
BEGIN
    cutoff_date := NOW() - INTERVAL '1 day' * days_old;
    
    DELETE FROM files 
    WHERE upload_status = 'FAILED' 
    AND created_at < cutoff_date;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Procedure to mark expired files
CREATE OR REPLACE FUNCTION mark_expired_files()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE files 
    SET upload_status = 'EXPIRED' 
    WHERE expires_at < NOW() 
    AND upload_status = 'COMPLETED';
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Procedure to clean up old access logs
CREATE OR REPLACE FUNCTION cleanup_old_access_logs(days_old INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMP WITH TIME ZONE;
BEGIN
    cutoff_date := NOW() - INTERVAL '1 day' * days_old;
    
    DELETE FROM file_access_logs 
    WHERE accessed_at < cutoff_date;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create indexes for better performance on cleanup operations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_files_upload_status_created_at 
    ON files(upload_status, created_at) 
    WHERE upload_status = 'FAILED';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_files_expires_at_status 
    ON files(expires_at, upload_status) 
    WHERE expires_at IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_file_access_logs_accessed_at_old 
    ON file_access_logs(accessed_at) 
    WHERE accessed_at < NOW() - INTERVAL '30 days';
