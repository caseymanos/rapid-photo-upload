-- Add performance metrics columns to upload_sessions table
ALTER TABLE upload_sessions
    ADD COLUMN IF NOT EXISTS total_bytes_uploaded BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS avg_upload_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS avg_throughput_mbps DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS min_upload_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS max_upload_duration_ms BIGINT;

-- Add index for querying by completion time
CREATE INDEX IF NOT EXISTS idx_upload_sessions_completed_at 
    ON upload_sessions(completed_at DESC) 
    WHERE status = 'COMPLETED';

-- Add comments for documentation
COMMENT ON COLUMN upload_sessions.total_bytes_uploaded IS 'Total bytes uploaded across all photos in this session';
COMMENT ON COLUMN upload_sessions.avg_upload_duration_ms IS 'Average upload duration per photo in milliseconds';
COMMENT ON COLUMN upload_sessions.avg_throughput_mbps IS 'Average throughput across all uploads in Mbps';
COMMENT ON COLUMN upload_sessions.min_upload_duration_ms IS 'Fastest photo upload duration in milliseconds';
COMMENT ON COLUMN upload_sessions.max_upload_duration_ms IS 'Slowest photo upload duration in milliseconds';
