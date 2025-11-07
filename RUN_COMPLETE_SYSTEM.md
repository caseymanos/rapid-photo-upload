# Run Complete System - Quick Start Guide

This guide walks you through running the complete RapidPhotoUpload system (backend + frontend) from scratch.

## Prerequisites

- ✅ Java 21 installed
- ✅ Maven 3.9+ installed
- ✅ Node.js 18+ installed
- ✅ Docker installed (for PostgreSQL)
- ✅ AWS credentials configured

## Step-by-Step Setup

### 1. Start PostgreSQL Database

```bash
docker run --name postgres-photoupload \
  -e POSTGRES_DB=photoupload \
  -e POSTGRES_USER=dbadmin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

**Verify it's running**:
```bash
docker ps | grep postgres-photoupload
```

### 2. Start Backend (Spring Boot)

Open a new terminal:

```bash
cd backend
./run.sh
```

Or manually:

```bash
cd backend
JAVA_HOME=/Users/caseymanos/Library/Java/JavaVirtualMachines/openjdk-21.0.1/Contents/Home mvn spring-boot:run
```

**Wait for**:
```
Started RapidPhotoUploadApplication in X.XXX seconds
```

**Verify it's running**:
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 3. Start Frontend (React)

Open another terminal:

```bash
cd frontend-web
npm run dev
```

**Wait for**:
```
  VITE v5.x.x  ready in XXX ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

### 4. Open Browser

Navigate to: **http://localhost:3000**

## First Time Usage

### Create Account

1. You'll be redirected to login page
2. Click **"create a new account"**
3. Enter:
   - Email: `test@example.com`
   - Password: `password123` (min 6 chars)
   - Confirm Password: `password123`
4. Click **"Create account"**
5. You'll be logged in automatically

### Upload Photos

1. After login, you'll see the **Upload Page**
2. Either:
   - **Drag and drop** photos into the upload zone
   - **Click** the upload zone to select files
3. Select multiple photos (try 10-20 to start)
4. Watch real-time progress:
   - Each file shows progress bar
   - 10 files upload simultaneously
   - Remaining files queued
5. Wait for all uploads to complete

### View Gallery

1. Click **"View Gallery"** button or **"Gallery"** tab
2. See all your uploaded photos in a grid
3. Click any photo to:
   - View full-size image
   - Add tags
   - Add description
   - See metadata (size, type, date)
4. Click **"Save Changes"** to update

## Test 100 Concurrent Uploads

### Prepare Test Files

Create 100 test images (or use existing photos):

```bash
# On macOS, create test images
mkdir test-photos
cd test-photos
for i in {1..100}; do
  # Create a simple test image (requires ImageMagick)
  # Or just copy existing photos
  cp ~/Pictures/sample.jpg photo-$i.jpg
done
```

### Run Test

1. Go to Upload Page
2. Click upload zone
3. Select all 100 photos
4. Observe:
   - ✅ All 100 files queued immediately
   - ✅ 10 uploading at a time
   - ✅ Progress bars updating in real-time
   - ✅ UI remains responsive
   - ✅ No errors or crashes
   - ✅ All 100 complete successfully

**Expected time**: 60-90 seconds (depends on internet speed)

### Verify Results

1. Go to Gallery
2. You should see all 100 photos
3. Click any photo to verify it displays correctly
4. Check backend logs for any errors

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Browser (http://localhost:3000)                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │           React Frontend                        │   │
│  │  - Upload Manager (10 concurrent)               │   │
│  │  - Real-time Progress Tracking                  │   │
│  │  - Photo Gallery                                │   │
│  └───────────────┬─────────────────────────────────┘   │
└──────────────────┼─────────────────────────────────────┘
                   │ HTTP/REST + JWT
                   │
┌──────────────────▼─────────────────────────────────────┐
│  Spring Boot Backend (http://localhost:8080)           │
│  ┌─────────────────────────────────────────────────┐   │
│  │  - Auth Controller (JWT)                        │   │
│  │  - Upload Controller (Presigned URLs)           │   │
│  │  - Photo Controller (CRUD)                      │   │
│  │  - S3 Integration                               │   │
│  └───────┬──────────────────────┬──────────────────┘   │
└──────────┼──────────────────────┼──────────────────────┘
           │                      │
    ┌──────▼──────┐        ┌──────▼──────┐
    │ PostgreSQL  │        │   AWS S3    │
    │ (metadata)  │        │  (photos)   │
    └─────────────┘        └─────────────┘
```

## Verification Checklist

### Backend Health

```bash
# Health check
curl http://localhost:8080/actuator/health

# Database connection
docker exec -it postgres-photoupload psql -U dbadmin -d photoupload -c "\dt"

# Check logs
tail -f backend/logs/application.log  # if configured
```

### Frontend Health

```bash
# Check if running
curl http://localhost:3000

# Check API proxy
curl http://localhost:3000/api/v1/actuator/health
```

### AWS S3

```bash
# List uploaded photos
aws s3 ls s3://rapid-photo-upload-dev/uploads/ --recursive

# Check bucket exists
aws s3 ls s3://rapid-photo-upload-dev/
```

## Troubleshooting

### Backend Won't Start

**Issue**: Port 8080 already in use
```bash
# Find process using port 8080
lsof -ti:8080

# Kill it
kill -9 <PID>
```

**Issue**: PostgreSQL connection failed
```bash
# Restart PostgreSQL container
docker restart postgres-photoupload

# Check logs
docker logs postgres-photoupload
```

**Issue**: Java version mismatch
```bash
# Check Java version
java -version

# Set JAVA_HOME
export JAVA_HOME=/path/to/java-21
```

### Frontend Won't Start

**Issue**: Port 3000 already in use
```bash
# Find process using port 3000
lsof -ti:3000

# Kill it
kill -9 <PID>
```

**Issue**: Dependencies not installed
```bash
cd frontend-web
rm -rf node_modules package-lock.json
npm install
```

### Upload Failures

**Issue**: S3 presigned URL errors
- Check AWS credentials in backend `application.yml`
- Verify S3 bucket exists and you have permissions
- Check bucket name matches configuration

**Issue**: 401 Unauthorized
- Clear browser localStorage (DevTools → Application → Local Storage)
- Re-login to get fresh JWT token

**Issue**: CORS errors
- Ensure backend allows `http://localhost:3000` in CORS config
- Check browser console for exact error

### Connection Issues

**Issue**: Backend connection refused
```bash
# Verify backend is running
curl http://localhost:8080/actuator/health

# Check Vite proxy in frontend-web/vite.config.ts
```

## Performance Expectations

### Upload Performance

- **Single file (2MB)**: ~2-5 seconds
- **10 concurrent (20MB total)**: ~10-15 seconds
- **100 concurrent (200MB total)**: ~60-90 seconds

*Times vary based on internet connection speed*

### UI Performance

- **Page load**: <1 second
- **Gallery render (100 photos)**: <2 seconds
- **Photo modal open**: <500ms
- **Responsive during uploads**: Always fluid

### Backend Performance

- **API response time**: <200ms (P95)
- **Database queries**: <50ms
- **S3 presigned URL generation**: <100ms

## Monitoring

### Backend Logs

```bash
# Spring Boot logs
tail -f backend/logs/application.log

# Or console output
# Look for errors, warnings, upload events
```

### Frontend Logs

- Open browser DevTools (F12)
- Console tab for logs
- Network tab for API calls

### Database

```bash
# Connect to PostgreSQL
docker exec -it postgres-photoupload psql -U dbadmin -d photoupload

# Check uploaded photos
SELECT id, original_filename, upload_status, created_at
FROM photos
ORDER BY created_at DESC
LIMIT 10;

# Check upload sessions
SELECT id, total_photos, completed_photos, failed_photos, status
FROM upload_sessions
ORDER BY started_at DESC;
```

## Stopping the System

### Stop Frontend

Press `Ctrl+C` in the frontend terminal

### Stop Backend

Press `Ctrl+C` in the backend terminal

### Stop PostgreSQL

```bash
docker stop postgres-photoupload

# To remove (data will be lost)
docker rm postgres-photoupload
```

## Quick Restart

```bash
# Terminal 1: PostgreSQL (if stopped)
docker start postgres-photoupload

# Terminal 2: Backend
cd backend && ./run.sh

# Terminal 3: Frontend
cd frontend-web && npm run dev
```

## Development Workflow

### Making Backend Changes

1. Edit Java files in `backend/src/`
2. Spring Boot will auto-reload (if using `mvn spring-boot:run`)
3. Or restart manually: `Ctrl+C` then `./run.sh`

### Making Frontend Changes

1. Edit React files in `frontend-web/src/`
2. Vite hot-reloads automatically
3. See changes instantly in browser

## Production Deployment

See separate deployment guides:
- `backend/DEPLOYMENT.md` (if exists)
- `frontend-web/README.md` for build instructions

Quick production build:

```bash
# Backend
cd backend
mvn clean package
# Produces: target/rapid-photo-upload-backend-1.0.0.jar

# Frontend
cd frontend-web
npm run build
# Produces: dist/ directory
```

## Support

For issues:
1. Check troubleshooting section above
2. Review logs (backend console, browser console)
3. Verify all services are running
4. Check configuration files

## Summary

You now have a fully functional photo upload system running locally:

✅ Backend API on port 8080
✅ Frontend app on port 3000
✅ PostgreSQL database in Docker
✅ AWS S3 integration
✅ 100+ concurrent upload capability
✅ Real-time progress tracking
✅ Photo gallery with metadata editing

**Ready to demonstrate high-volume photo uploads!**
