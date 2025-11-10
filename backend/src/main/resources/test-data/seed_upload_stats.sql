-- Seed data for testing upload statistics feature
-- This script creates sample upload sessions with realistic performance metrics

-- Note: This script assumes you already have a test user account.
-- If not, create one through the registration endpoint first.
-- Replace the user_id below with your actual test user's UUID.

-- Example: If you need to create a test user (uncomment and run separately):
-- INSERT INTO users (id, email, password_hash, created_at, updated_at) 
-- VALUES (
--   '00000000-0000-0000-0000-000000000001', 
--   'test@rapidphotoupload.com', 
--   '$2a$10$dummyHashForTestingOnly', 
--   NOW(), 
--   NOW()
-- ) ON CONFLICT (email) DO NOTHING;

-- Create sample upload sessions with varying characteristics
-- Replace '00000000-0000-0000-0000-000000000001' with your actual user ID

-- Session 1: Large successful upload (10 photos, 2 days ago)
INSERT INTO upload_sessions (
  id, 
  user_id, 
  session_token, 
  total_photos, 
  completed_photos, 
  failed_photos, 
  status, 
  started_at, 
  completed_at, 
  total_bytes_uploaded, 
  avg_upload_duration_ms, 
  avg_throughput_mbps, 
  min_upload_duration_ms, 
  max_upload_duration_ms,
  version
)
VALUES (
  gen_random_uuid(), 
  '00000000-0000-0000-0000-000000000001', 
  'session_' || substr(md5(random()::text), 1, 16), 
  10, 
  10, 
  0, 
  'COMPLETED', 
  NOW() - INTERVAL '2 days 15 minutes', 
  NOW() - INTERVAL '2 days', 
  52428800, -- 50 MB
  2500, 
  167.77, 
  1800, 
  4200,
  0
);

-- Session 2: Medium upload with one failure (5 photos, 1 day ago)
INSERT INTO upload_sessions (
  id, 
  user_id, 
  session_token, 
  total_photos, 
  completed_photos, 
  failed_photos, 
  status, 
  started_at, 
  completed_at, 
  total_bytes_uploaded, 
  avg_upload_duration_ms, 
  avg_throughput_mbps, 
  min_upload_duration_ms, 
  max_upload_duration_ms,
  version
)
VALUES (
  gen_random_uuid(), 
  '00000000-0000-0000-0000-000000000001', 
  'session_' || substr(md5(random()::text), 1, 16), 
  5, 
  4, 
  1, 
  'COMPLETED', 
  NOW() - INTERVAL '1 day 8 minutes', 
  NOW() - INTERVAL '1 day', 
  26214400, -- 25 MB
  3000, 
  69.91, 
  2100, 
  4800,
  0
);

-- Session 3: Small fast upload (3 photos, 12 hours ago)
INSERT INTO upload_sessions (
  id, 
  user_id, 
  session_token, 
  total_photos, 
  completed_photos, 
  failed_photos, 
  status, 
  started_at, 
  completed_at, 
  total_bytes_uploaded, 
  avg_upload_duration_ms, 
  avg_throughput_mbps, 
  min_upload_duration_ms, 
  max_upload_duration_ms,
  version
)
VALUES (
  gen_random_uuid(), 
  '00000000-0000-0000-0000-000000000001', 
  'session_' || substr(md5(random()::text), 1, 16), 
  3, 
  3, 
  0, 
  'COMPLETED', 
  NOW() - INTERVAL '12 hours 5 minutes', 
  NOW() - INTERVAL '12 hours', 
  15728640, -- 15 MB
  1800, 
  69.91, 
  1500, 
  2200,
  0
);

-- Session 4: Large batch upload (20 photos, 3 days ago)
INSERT INTO upload_sessions (
  id, 
  user_id, 
  session_token, 
  total_photos, 
  completed_photos, 
  failed_photos, 
  status, 
  started_at, 
  completed_at, 
  total_bytes_uploaded, 
  avg_upload_duration_ms, 
  avg_throughput_mbps, 
  min_upload_duration_ms, 
  max_upload_duration_ms,
  version
)
VALUES (
  gen_random_uuid(), 
  '00000000-0000-0000-0000-000000000001', 
  'session_' || substr(md5(random()::text), 1, 16), 
  20, 
  18, 
  2, 
  'COMPLETED', 
  NOW() - INTERVAL '3 days 45 minutes', 
  NOW() - INTERVAL '3 days', 
  104857600, -- 100 MB
  3200, 
  262.14, 
  2000, 
  5500,
  0
);

-- Session 5: Recent upload in progress (2 photos, still uploading)
INSERT INTO upload_sessions (
  id, 
  user_id, 
  session_token, 
  total_photos, 
  completed_photos, 
  failed_photos, 
  status, 
  started_at, 
  completed_at, 
  total_bytes_uploaded, 
  avg_upload_duration_ms, 
  avg_throughput_mbps, 
  min_upload_duration_ms, 
  max_upload_duration_ms,
  version
)
VALUES (
  gen_random_uuid(), 
  '00000000-0000-0000-0000-000000000001', 
  'session_' || substr(md5(random()::text), 1, 16), 
  5, 
  2, 
  0, 
  'IN_PROGRESS', 
  NOW() - INTERVAL '5 minutes', 
  NULL, 
  10485760, -- 10 MB (partial)
  2200, 
  38.15, 
  2000, 
  2400,
  0
);

-- Session 6: Failed session (network issues, 1 week ago)
INSERT INTO upload_sessions (
  id, 
  user_id, 
  session_token, 
  total_photos, 
  completed_photos, 
  failed_photos, 
  status, 
  started_at, 
  completed_at, 
  total_bytes_uploaded, 
  avg_upload_duration_ms, 
  avg_throughput_mbps, 
  min_upload_duration_ms, 
  max_upload_duration_ms,
  version
)
VALUES (
  gen_random_uuid(), 
  '00000000-0000-0000-0000-000000000001', 
  'session_' || substr(md5(random()::text), 1, 16), 
  8, 
  3, 
  5, 
  'FAILED', 
  NOW() - INTERVAL '7 days 10 minutes', 
  NOW() - INTERVAL '7 days', 
  15728640, -- 15 MB (partial)
  2800, 
  44.98, 
  2500, 
  3100,
  0
);

-- Verify the data was inserted
SELECT 
  id,
  session_token,
  total_photos,
  completed_photos,
  failed_photos,
  status,
  total_bytes_uploaded / 1024 / 1024 AS total_mb,
  avg_upload_duration_ms,
  avg_throughput_mbps,
  started_at,
  completed_at
FROM upload_sessions
WHERE user_id = '00000000-0000-0000-0000-000000000001'
ORDER BY started_at DESC;

-- Expected aggregate stats:
-- Total sessions: 6 (5 completed + 1 in progress)
-- Total photos: 51 (40 completed + 6 failed + 5 in progress)
-- Total bytes: ~225 MB
-- Success rate: ~87% (40/46 completed photos)
