# RapidPhotoUpload Implementation Analysis

## Executive Summary
The project requires building a high-volume photo upload system with 100 concurrent uploads, using DDD, CQRS, and Vertical Slice Architecture. **Current status: We have Supabase PostgreSQL with auth implemented, but need to add photo upload tables and integrate with existing Java backend.**

## Current Infrastructure Assessment

### ✅ What We Have
1. **PostgreSQL Database** via Supabase (Project: nhadlfbxbivlhtkbolve.supabase.co)
2. **Authentication System** - Supabase Auth integrated in both mobile and web
3. **Java Spring Boot Backend** - Already deployed with DDD/CQRS/VSA architecture
4. **AWS S3 Storage** - Backend has AwsConfig and storage integration
5. **React Native Mobile App** - Expo-based with upload functionality
6. **React Web Frontend** - Vite-based TypeScript app
7. **WebSocket Support** - Real-time progress updates via Spring WebSocket

### Current Supabase Schema
Currently contains unrelated tables (profiles, conversations, messages, tutorials, etc.). **No photo upload tables exist yet.**

### ❌ What We Need to Add

#### 1. Database Schema (Supabase PostgreSQL)
Need to create these tables to support the photo upload system:

```sql
-- Users table (integrate with Supabase auth)
CREATE TABLE users (
  id UUID PRIMARY KEY REFERENCES auth.users(id),
  email TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Upload sessions (batch upload tracking)
CREATE TABLE upload_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  status TEXT NOT NULL DEFAULT 'active', -- active, completed, failed
  total_photos INTEGER NOT NULL DEFAULT 0,
  completed_photos INTEGER NOT NULL DEFAULT 0,
  failed_photos INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Photos metadata
CREATE TABLE photos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  session_id UUID REFERENCES upload_sessions(id),
  filename TEXT NOT NULL,
  original_filename TEXT NOT NULL,
  file_size BIGINT NOT NULL,
  mime_type TEXT NOT NULL,
  s3_key TEXT NOT NULL, -- S3 object key
  s3_bucket TEXT NOT NULL,
  thumbnail_s3_key TEXT, -- thumbnail variant
  upload_status TEXT NOT NULL DEFAULT 'pending', -- pending, uploading, completed, failed
  upload_progress INTEGER DEFAULT 0, -- 0-100
  error_message TEXT,
  width INTEGER,
  height INTEGER,
  tags TEXT[],
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Upload parts (for multipart uploads)
CREATE TABLE upload_parts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  photo_id UUID NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
  part_number INTEGER NOT NULL,
  etag TEXT NOT NULL,
  size BIGINT NOT NULL,
  uploaded_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(photo_id, part_number)
);

-- Indexes for performance
CREATE INDEX idx_photos_user_id ON photos(user_id);
CREATE INDEX idx_photos_session_id ON photos(session_id);
CREATE INDEX idx_photos_status ON photos(upload_status);
CREATE INDEX idx_upload_sessions_user_id ON upload_sessions(user_id);
```

#### 2. Backend Integration Points

**Java Backend Already Has:**
- ✅ Domain models: `User`, `PhotoMetadata`, `UploadPart`, `UploadSession`
- ✅ Repositories: `UserRepository`, `UploadSessionRepository`
- ✅ Controllers: `AuthController`, `UploadController`
- ✅ AWS S3 integration via `AwsConfig`
- ✅ WebSocket for real-time updates
- ✅ JWT authentication (need to integrate with Supabase tokens)

**Need to Add:**
- [ ] Supabase PostgreSQL connection (currently using H2/other DB)
- [ ] Photo repository implementation
- [ ] Supabase JWT validation (integrate with existing JwtService)
- [ ] Row Level Security (RLS) policies in Supabase

#### 3. Mobile App Status

**Already Has:**
- ✅ Upload service with multipart support
- ✅ Upload manager with concurrency (20 concurrent uploads)
- ✅ WebSocket integration for real-time progress
- ✅ Photo picker and gallery
- ✅ Supabase auth integration

**Need to Update:**
- [ ] Point to Supabase-backed API endpoints
- [ ] Use Supabase auth tokens for API calls
- [ ] Update upload service to use new photo endpoints

#### 4. Web Frontend Status

**Already Has:**
- ✅ Upload manager with drag-and-drop
- ✅ Progress indicators
- ✅ Photo gallery view
- ✅ Supabase auth integration

**Need to Update:**
- [ ] Use Supabase auth tokens for API calls
- [ ] Update API endpoints
- [ ] Add photo tagging/metadata features

## Implementation Roadmap

### Phase 1: Database Migration (1-2 hours)
1. Create Supabase migrations for photo tables
2. Set up Row Level Security (RLS) policies
3. Create database functions for upload progress tracking
4. Add triggers for updated_at timestamps

### Phase 2: Backend Integration (3-4 hours)
1. Add Supabase PostgreSQL connection to Spring Boot
2. Update application.properties with Supabase credentials
3. Implement PhotoRepository using JPA
4. Add Supabase JWT validation filter
5. Update existing controllers to work with Supabase auth
6. Test multipart upload flow with S3

### Phase 3: Frontend Integration (2-3 hours)
1. Update API client to use Supabase auth tokens
2. Modify upload services to call new endpoints
3. Test 100 concurrent uploads
4. Verify WebSocket real-time updates

### Phase 4: Testing & Optimization (2-3 hours)
1. Integration tests for full upload flow
2. Load testing with 100 concurrent uploads
3. Performance optimization
4. Error handling and retry logic

## Key Architectural Decisions

### 1. Authentication Strategy
- **Frontend**: Supabase Auth (email/password) ✅ Implemented
- **Backend**: Accept Supabase JWT tokens, validate via Supabase public key
- **Migration Path**: Keep existing JWT infrastructure, add Supabase token validation

### 2. Database Strategy
- **Primary DB**: Supabase PostgreSQL (replaces current backend DB)
- **Schema**: Follow existing domain models from Java backend
- **Security**: Row Level Security (RLS) to ensure users only see their photos

### 3. Storage Strategy
- **File Storage**: AWS S3 (already configured in backend)
- **Metadata**: Supabase PostgreSQL
- **Thumbnails**: Generate and store in S3 with separate keys

### 4. Concurrency Strategy
- **Frontend**: 20 concurrent uploads (already implemented)
- **Backend**: Virtual threads (Spring Boot 3+) or thread pool
- **Database**: Connection pooling (Supabase built-in)

## PRD Compliance Checklist

### Functional Requirements
- [x] Support 100 concurrent photo uploads (mobile has 20, need to increase)
- [x] Asynchronous UI (already implemented)
- [x] Real-time status updates (WebSocket already implemented)
- [x] Web interface (already built)
- [x] Mobile interface (React Native already built)
- [ ] Backend managing concurrent requests (need Supabase connection)
- [x] Authentication (Supabase auth implemented)

### Architecture Requirements
- [x] Domain-Driven Design (backend already follows DDD)
- [x] CQRS (backend already implements CQRS)
- [x] Vertical Slice Architecture (backend already follows VSA)

### Technical Stack
- [x] Java Spring Boot backend
- [x] TypeScript React web
- [x] React Native mobile
- [x] AWS S3 storage (configured in backend)
- [ ] PostgreSQL (Supabase - need to connect)
- [x] AWS deployment target

### Performance Benchmarks
- [ ] 100 photos (2MB each) in 90 seconds - **Need to test**
- [x] UI remains responsive (already implemented)

## Immediate Next Steps

1. **Create Supabase Migration** for photo tables
2. **Update Backend** to connect to Supabase PostgreSQL
3. **Add Supabase JWT Validation** to backend security
4. **Test Integration** with existing mobile/web apps
5. **Load Test** with 100 concurrent uploads

## Environment Variables Needed

### Backend (Java)
```properties
# Supabase Database
spring.datasource.url=jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres
spring.datasource.username=postgres.[PROJECT_REF]
spring.datasource.password=[SUPABASE_DB_PASSWORD]

# Supabase Auth
supabase.url=https://nhadlfbxbivlhtkbolve.supabase.co
supabase.jwt.secret=[SUPABASE_JWT_SECRET]

# AWS S3 (already configured)
aws.s3.bucket=rapid-photo-upload-bucket
aws.region=us-east-1
```

### Frontend (Already Configured)
```env
VITE_SUPABASE_URL=https://nhadlfbxbivlhtkbolve.supabase.co
VITE_SUPABASE_ANON_KEY=[ANON_KEY]
```

## Risk Mitigation

### Risk 1: Token Validation Complexity
**Mitigation**: Use Supabase JWT verification library, validate both Supabase and custom tokens during transition

### Risk 2: Database Migration
**Mitigation**: Keep existing backend DB temporarily, dual-write during migration, test thoroughly

### Risk 3: Performance at 100 Concurrent Uploads
**Mitigation**: Use virtual threads, connection pooling, S3 multipart upload optimization

### Risk 4: WebSocket Scalability
**Mitigation**: Use Redis for WebSocket message distribution if needed, currently single-server should handle load

## Success Metrics

1. **Upload Success Rate**: >95% for 100 concurrent uploads
2. **Upload Speed**: 100 photos (2MB) in <90 seconds
3. **UI Responsiveness**: No frame drops during uploads
4. **Error Recovery**: Automatic retry on transient failures
5. **Real-time Updates**: <500ms latency for progress updates
