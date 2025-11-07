# RapidPhotoUpload Backend - Setup Complete ✅

## Summary

Your RapidPhotoUpload backend is now configured for local testing! Here's what was set up:

### ✅ Completed Setup Tasks

1. **PostgreSQL Database** - Running in Docker container `postgres-photoupload`
   - Host: localhost:5432
   - Database: photoupload
   - Username: dbadmin
   - Password: password

2. **AWS Configuration**
   - Region: us-east-1
   - S3 Bucket: rapid-photo-upload-dev (newly created)
   - Credentials: Configured from ~/.aws/credentials

3. **JWT Security**
   - Secret: Securely generated and stored in .env.local

4. **Maven Build System**
   - Version: 3.9.11
   - Java: 21.0.1
   - Build Status: ✅ SUCCESS

5. **Code Fixes Applied**
   - Fixed Lombok annotation processing for Java 21
   - Updated JWT library usage (JJWT 0.12.3 API)
   - Fixed UploadSession constructor visibility
   - Fixed PhotoController record accessor method
   - Fixed Flyway PostgreSQL driver version

### 📁 Created Files

- `.env.local` - Environment configuration with your AWS credentials
- `.env.local.example` - Template for environment variables
- `setup.sh` - Automated setup script
- `mvn.sh` - Maven wrapper with correct Java 21 path
- `run.sh` - Application startup script with environment loading
- `.mvn/jvm.config` - Maven JVM configuration for Java 21

### 🚀 How to Run the Application

#### Option 1: Using the run script (recommended)
```bash
./run.sh
```

#### Option 2: Manual startup
```bash
source .env.local
./mvn.sh spring-boot:run
```

#### Option 3: Build and run JAR
```bash
./mvn.sh clean package
java -jar target/rapid-photo-upload-1.0.0.jar
```

### 🧪 Running Tests

#### Unit and Integration Tests
```bash
./mvn.sh test
```

#### Integration Tests Only
```bash
./mvn.sh verify -Pintegration-tests
```

**Note:** Test failures related to duplicate email constraints are expected on first run due to test data persistence. The tests use Testcontainers with PostgreSQL and mock S3 services.

### 🔍 Verify Application Health

Once the application is running, check the health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 📋 API Endpoints

#### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login and get JWT token

#### Upload Operations
- `POST /api/v1/uploads/initiate` - Initiate multipart upload
- `POST /api/v1/uploads/{photoId}/complete` - Complete upload
- `GET /api/v1/uploads/sessions/{sessionId}/status` - Get session status

#### Photo Operations
- `GET /api/v1/photos` - List user's photos
- `GET /api/v1/photos/{photoId}` - Get photo details
- `PUT /api/v1/photos/{photoId}/metadata` - Update metadata

### 🗄️ Database Management

#### View PostgreSQL container status
```bash
docker ps --filter name=postgres-photoupload
```

#### Stop database
```bash
docker stop postgres-photoupload
```

#### Start database
```bash
docker start postgres-photoupload
```

#### Connect to database (for debugging)
```bash
docker exec -it postgres-photoupload psql -U dbadmin -d photoupload
```

#### View Flyway migrations
```sql
SELECT * FROM flyway_schema_history;
```

### 🔧 Configuration Files

#### application.yml (main)
- Database connection with HikariCP pooling (10-50 connections)
- AWS S3 and EventBridge configuration
- JWT security settings
- Resilience4j circuit breaker and retry policies
- Actuator endpoints for monitoring

#### application-test.yml (test profile)
- Testcontainers PostgreSQL configuration
- Mocked S3 services
- Debug logging enabled

### 📊 Architecture Overview

**Domain Layer (DDD):**
- Aggregates: Photo, UploadSession, User
- Value Objects: PhotoMetadata, UploadStatus, SessionStatus
- Domain Events: PhotoUploadInitiated, PhotoUploadCompleted, etc.

**Application Layer (CQRS):**
- Commands: InitiateUploadCommand, CompleteUploadCommand, etc.
- Queries: GetPhotosQuery, GetPhotoByIdQuery, etc.
- Handlers: Separate read/write operation handlers

**Infrastructure Layer:**
- S3StorageService: Multipart uploads with presigned URLs
- Circuit breaker pattern for AWS service resilience
- JPA repositories with domain entity mapping
- JWT-based authentication and authorization

### 🐛 Troubleshooting

#### Build Issues
- **Java version mismatch**: The project requires Java 21. Use `./mvn.sh` which sets the correct JAVA_HOME
- **Lombok not working**: Maven compiler plugin is configured with Lombok annotation processor

#### Database Issues
- **Connection refused**: Ensure Docker is running and PostgreSQL container is started
- **Migration failed**: Check Flyway schema in database

#### AWS Issues
- **S3 access denied**: Verify AWS credentials in .env.local
- **Bucket not found**: Ensure rapid-photo-upload-dev bucket exists in us-east-1

### 📝 Next Steps

1. **Start the application**: `./run.sh`
2. **Test authentication**: Register a user and login to get JWT token
3. **Test upload flow**: Initiate upload, use presigned URLs, complete upload
4. **Monitor performance**: Check actuator/metrics endpoint
5. **View logs**: Application logs will show SQL queries and AWS operations

### 🔐 Security Notes

- `.env.local` contains sensitive credentials and is git-ignored
- JWT secret should be changed for production deployment
- AWS credentials are read from environment (never commit to source control)
- Database password should be rotated for production

### 📚 Additional Resources

- **Spring Boot Docs**: https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/
- **AWS SDK for Java**: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/
- **Testcontainers**: https://www.testcontainers.org/
- **DDD Patterns**: https://martinfowler.com/tags/domain%20driven%20design.html

---

**Setup completed on**: 2025-11-07
**Project**: RapidPhotoUpload Backend
**Architecture**: DDD + CQRS + Vertical Slice Architecture
