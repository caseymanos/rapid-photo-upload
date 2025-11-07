# Quick Start Guide

## Prerequisites Check

Before starting, ensure:
- ✅ Docker is running
- ✅ PostgreSQL container is running: `docker ps | grep postgres-photoupload`
- ✅ Port 8080 is available

## Start the Application

```bash
# From the backend directory
./run.sh
```

The application will:
1. Load environment variables from `.env.local`
2. Connect to PostgreSQL database
3. Run Flyway migrations
4. Initialize AWS S3 client
5. Start on http://localhost:8080

**Expected startup time:** ~10-15 seconds

## Verify It's Running

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

## Run Quick Tests

```bash
# Test all main endpoints
./test-endpoints.sh
```

## Common Commands

### Database
```bash
# View database logs
docker logs postgres-photoupload

# Stop database
docker stop postgres-photoupload

# Start database
docker start postgres-photoupload

# Reset database (removes all data)
docker rm -f postgres-photoupload
docker run --name postgres-photoupload \
  -e POSTGRES_DB=photoupload \
  -e POSTGRES_USER=dbadmin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

### Application
```bash
# Build only (no tests)
./mvn.sh clean install -DskipTests

# Run tests
./mvn.sh test

# Package as JAR
./mvn.sh clean package

# Run JAR directly
java -jar target/rapid-photo-upload-1.0.0.jar
```

## Example API Calls

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "username": "johndoe"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'

# Save the token from response
TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

### 3. Initiate Upload

```bash
curl -X POST http://localhost:8080/api/v1/uploads/initiate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "photo.jpg",
    "contentType": "image/jpeg",
    "fileSize": 2097152,
    "metadata": {
      "description": "My photo",
      "tags": ["vacation", "beach"]
    }
  }'
```

### 4. Get Photos

```bash
curl -X GET http://localhost:8080/api/v1/photos \
  -H "Authorization: Bearer $TOKEN"
```

## Troubleshooting

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Database Connection Failed
```bash
# Check if container is running
docker ps | grep postgres

# If not running, start it
docker start postgres-photoupload

# Check logs
docker logs postgres-photoupload
```

### AWS S3 Errors
```bash
# Verify credentials are loaded
source .env.local
echo $AWS_ACCESS_KEY_ID

# Test S3 access
aws s3 ls s3://rapid-photo-upload-dev
```

### Build Errors
```bash
# Clean build
./mvn.sh clean

# Rebuild from scratch
./mvn.sh clean install -DskipTests

# Check Java version
./mvn.sh --version
# Should show Java 21.0.1
```

## Stopping the Application

Press `Ctrl+C` in the terminal where the application is running.

Or if running in background:
```bash
# Find the process
ps aux | grep spring-boot

# Kill it
kill <PID>
```

## Next Steps

1. ✅ Application is running
2. 📖 Read full documentation: `SETUP_COMPLETE.md`
3. 🧪 Run integration tests: `./mvn.sh verify`
4. 📊 Monitor metrics: http://localhost:8080/actuator/metrics
5. 🔍 View Prometheus metrics: http://localhost:8080/actuator/prometheus

## Need Help?

- Check logs in console output
- Review `SETUP_COMPLETE.md` for detailed troubleshooting
- Verify `.env.local` has correct values
- Ensure Docker and PostgreSQL are running
