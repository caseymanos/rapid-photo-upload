# RapidPhotoUpload Backend

High-performance photo upload system built with Spring Boot, following Domain-Driven Design (DDD), CQRS, and Vertical Slice Architecture (VSA) principles.

## ✅ Setup Status: READY FOR TESTING

**Last configured:** 2025-11-07

### Quick Start
```bash
./run.sh
```

For detailed setup information, see [QUICK_START.md](QUICK_START.md) or [SETUP_COMPLETE.md](SETUP_COMPLETE.md).

## Architecture Overview

### Domain-Driven Design (DDD)
- **Aggregates**: `Photo`, `UploadSession`, `User`
- **Value Objects**: `PhotoMetadata`, `UploadStatus`
- **Domain Events**: `PhotoUploadInitiated`, `PhotoUploadCompleted`, `PhotoUploadFailed`

### CQRS Pattern
- **Commands**: `InitiateUploadCommand`, `CompleteUploadCommand`, `UpdatePhotoMetadataCommand`
- **Queries**: `GetPhotosQuery`, `GetUploadStatusQuery`
- Clear separation between write and read operations

### Vertical Slice Architecture
Features organized by business capability:
- `upload/` - Upload initiation and completion
- `photo/` - Photo metadata and gallery operations
- `auth/` - Authentication and authorization

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 15+
- AWS Account (S3 access)
- Docker (for local development)

## Local Setup

### 1. Database Setup

```bash
docker run --name postgres-photoupload \
  -e POSTGRES_DB=photoupload \
  -e POSTGRES_USER=dbadmin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

### 2. AWS Configuration

Set up AWS credentials:
```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export S3_BUCKET_NAME=rapid-photo-upload-dev
```

### 3. Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Upload Operations
```
POST /api/v1/uploads/initiate
POST /api/v1/uploads/{photoId}/complete
GET  /api/v1/uploads/sessions/{sessionId}/status
```

### Photo Operations
```
GET  /api/v1/photos
GET  /api/v1/photos/{photoId}
PUT  /api/v1/photos/{photoId}/metadata
```

## Key Features

- **100 Concurrent Uploads**: Handles high-volume concurrent photo uploads
- **Direct S3 Upload**: Presigned URLs enable client-to-S3 upload (bandwidth offload)
- **Multipart Upload**: 5MB chunks for efficient large file handling
- **Async Processing**: ThreadPoolTaskExecutor configured for 100 concurrent operations
- **Circuit Breaker**: Resilience4j protects against S3 service failures
- **Optimistic Locking**: Prevents race conditions on concurrent updates
- **Event-Driven**: Domain events published to EventBridge for post-processing

## Performance Benchmarks

- **Target**: 100 photos (2MB each) uploaded in <90 seconds
- **API Latency**: P95 < 200ms
- **Database Connection Pool**: 10-50 connections
- **Thread Pool**: 50 core, 100 max threads

## Testing

Run unit tests:
```bash
mvn test
```

Run integration tests:
```bash
mvn verify
```

## Project Structure

```
src/main/java/com/rapidphotoupload/
├── domain/              # Domain Layer (DDD)
│   ├── model/          # Aggregates, entities, value objects
│   ├── event/          # Domain events
│   └── repository/     # Repository interfaces
├── application/         # Application Layer (CQRS)
│   ├── command/        # Write operations
│   ├── query/          # Read operations
│   ├── handler/        # Command/query handlers
│   └── dto/            # Data transfer objects
├── feature/            # Vertical Slices
│   ├── upload/         # Upload operations
│   ├── photo/          # Photo gallery
│   └── auth/           # Authentication
├── infrastructure/     # Infrastructure Layer
│   ├── persistence/    # JPA repositories
│   ├── storage/        # S3 integration
│   ├── event/          # EventBridge publisher
│   └── config/         # Configuration
└── shared/             # Shared kernel
    ├── exception/
    └── util/
```

## License

MIT