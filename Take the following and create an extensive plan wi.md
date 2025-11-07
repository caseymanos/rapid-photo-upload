<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# Take the following and create an extensive plan with architecture. I will use AWS. let me know if you have any quesitons. I basically want a schema, architecture. and epics broken down into stories.  Product Requirements Document (PRD): RapidPhotoUpload — AI-Assisted High-Volume Photo Upload System

1. Introduction and Project Goal
1.1 Project Goal
The goal of the RapidPhotoUpload project is to design and implement a high-performance, asynchronous photo upload system capable of reliably handling up to 100 concurrent media uploads. This project challenges candidates to demonstrate architectural excellence, mastery of concurrency, and exceptional user experience design across both mobile and web clients.
1.2 Context
This system simulates a high-volume media platform (similar to Google Photos or Drive) where users upload large batches of images while expecting the application to remain fully responsive. The project focuses on handling load, providing real-time feedback, and ensuring a clean, scalable design suitable for production environments.
2. Business Functionality
2.1 Problem Statement
Users expect seamless, high-speed media uploads without application freezing. The system must address the architectural complexities of concurrent file handling, status tracking, and efficient storage integration to deliver a reliable, non-blocking experience.
2.2 Core Functional Requirements
High-Volume Concurrency: The system MUST support the simultaneous uploading of up to 100 photos per user session.
Asynchronous UI: Users MUST be able to continue navigating and interacting with the application (both web and mobile) while uploads are in progress.
Real-Time Status: Display individual and batch upload progress using responsive indicators (e.g., progress bars) with real-time status updates (Uploading, Failed, Complete).
Web Interface: A dedicated web interface for viewing, tagging, and downloading previously uploaded photos.
Mobile Interface: A dedicated mobile application (React Native or Flutter) that mirrors the upload and viewing functionality.
Backend Handling: The backend must manage concurrent requests, store file metadata, and efficiently stream/store the binary files in cloud object storage.
Authentication: Basic authentication (mocked or JWT-based) is required to secure access for both mobile and web clients.
Project Scope: This is a two-part project consisting of one mobile application and one web application, both integrated with the same shared backend API.
3. Architecture and Technical Requirements
3.1 Architectural Principles (Mandatory)
The backend architecture is the core of the assessment and MUST adhere to the following principles:
Domain-Driven Design (DDD): Core concepts (e.g., Photo, Upload Job, User) must be modeled as robust Domain Objects.
CQRS (Command Query Responsibility Segregation): Implement a clear separation between handling upload/mutation commands and querying photo status/metadata.
Vertical Slice Architecture (VSA): Organize the backend code around features (e.g., UploadPhotoSlice, GetPhotoMetadataSlice).
3.2 Technical Stack
Back-End (API): Java with Spring Boot. Must handle large, asynchronous requests efficiently.
Web Front-End: TypeScript with React.js.
Mobile Front-End: React Native or Flutter.
Cloud Storage (Mandatory): Files MUST be stored in a scalable object storage solution: AWS S3 or Azure Blob Storage.
Database: PostgreSQL is required for persisting metadata (User, Photo, Upload Job Status).
Cloud Platforms: Deployment target flexibility: AWS or Azure.
3.3 Performance Benchmarks
Concurrency Load: The system MUST handle the concurrent upload of 100 photos (average size 2MB each) within 90 seconds on a standard broadband connection.
UI Responsiveness: Both the mobile and web interfaces MUST remain fluid and fully responsive during peak upload operations.
4. Code Quality and AI Acceleration
4.1 Code Quality Standards (Mandatory)
Architecture: Clean separation of concerns across Domain, Application, and Infrastructure layers.
Backend: Must demonstrate robust handling of concurrency, including mechanisms for retries and efficient streaming of large file uploads.
Frontend: Both React/Next.js and React Native/Flutter apps must use a clean, component-based architecture and adhere strictly to TypeScript standards.
Readability: Consistent naming conventions, modularity, and comprehensive documentation are required.
4.2 Testing (Mandatory)
Integration Tests: MUST implement integration tests that validate the complete upload process, from the client (simulated mobile/web) through the backend services and ending with successful persistent storage in the cloud object store.
4.3 AI Tool Utilization
AI tools (Cursor, Copilot, v0.dev, Locofy) are optional. If used, they should be applied intelligently for tasks such as:
Image categorization (e.g., tagging the uploaded image contents).
Compression optimization algorithms.
Upload prioritization logic.
The evaluation will measure the candidate's effective use of AI for quality acceleration.
5. Project Deliverables and Constraints
5.1 Time Constraint
Recommended Completion Time: 5 days.
5.2 Submission Requirements
Code Repository: Complete, functional code repository (GitHub preferred), containing all three components (backend, web client, mobile client).
Brief Technical Writeup (1-2 pages): Documenting the chosen concurrency strategy, asynchronous design, cloud storage interaction (S3/Blob), and the division of logic across the three application components.
Demo: A video or live presentation demonstrating the simultaneous upload of a batch of images and the real-time progress indicators on both client platforms.
AI Tool Documentation: Detailed documentation of any AI tools used, including example prompts and a justification for their impact.
Test Cases and Validation Results: Evidence of passing integration tests validating the end-to-end upload flow.

Perfect! Now I have enough context. Let me create a comprehensive architecture and implementation plan.

***

# RapidPhotoUpload: Complete Architecture \& Implementation Plan

## Executive Summary

This document provides a comprehensive architecture, database schema, AWS infrastructure design, and detailed epic/story breakdown for the RapidPhotoUpload system. The solution leverages AWS services (S3, RDS PostgreSQL, SQS, EventBridge, CloudFront) with Spring Boot backend and React/React Native frontends, following DDD, CQRS, and Vertical Slice Architecture principles.

***

## 1. System Architecture

### 1.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Layer                            │
├──────────────────────────────┬──────────────────────────────────┤
│   React Web Application      │  React Native Mobile App         │
│   - TypeScript               │  - TypeScript                    │
│   - Upload Manager           │  - Background Upload Service     │
│   - Progress Tracking        │  - Offline Queue                 │
└──────────────┬───────────────┴──────────────┬───────────────────┘
               │                              │
               │  HTTPS/REST API             │
               └──────────────┬───────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                     API Gateway Layer (AWS)                      │
│  - Application Load Balancer (ALB)                              │
│  - CloudFront (CDN for static assets)                           │
│  - WAF (Web Application Firewall)                               │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│              Spring Boot Application (ECS Fargate)               │
├──────────────────────────────────────────────────────────────────┤
│  API Layer                                                       │
│    └─ REST Controllers (per vertical slice)                     │
│                                                                  │
│  Application Layer (CQRS)                                        │
│    ├─ Commands: InitiateUploadCommand, CompleteUploadCommand    │
│    ├─ Queries: GetPhotosQuery, GetUploadStatusQuery             │
│    └─ Command/Query Handlers                                    │
│                                                                  │
│  Domain Layer (DDD)                                              │
│    ├─ Aggregates: Photo, UploadSession, User                    │
│    ├─ Value Objects: PhotoMetadata, UploadStatus                │
│    ├─ Domain Events: PhotoUploadInitiated, PhotoUploadCompleted │
│    └─ Repository Interfaces                                     │
│                                                                  │
│  Infrastructure Layer                                            │
│    ├─ Repository Implementations (JPA)                          │
│    ├─ S3 Integration Service                                    │
│    ├─ Event Publishing (EventBridge)                            │
│    └─ Async Task Executors                                      │
└──────────────┬──────────────────────────────┬────────────────────┘
               │                              │
       ┌───────▼────────┐            ┌───────▼────────┐
       │  RDS PostgreSQL│            │   AWS S3       │
       │  - Metadata    │            │  - Photo       │
       │  - Upload Jobs │            │    Storage     │
       │  - User Data   │            │                │
       └────────────────┘            └────────┬───────┘
                                              │
                                     ┌────────▼────────┐
                                     │  S3 Events →    │
                                     │  EventBridge    │
                                     └────────┬────────┘
                                              │
                                     ┌────────▼────────┐
                                     │  SQS Queue      │
                                     │  (Post-process) │
                                     └─────────────────┘
```


### 1.2 Key Architectural Decisions

#### **1.2.1 Upload Strategy: Client-Side Direct Upload with Presigned URLs**

**Pattern**: S3 Multipart Upload with Presigned URLs

```
Flow:
1. Client → Backend: Request upload initiation
2. Backend → S3: Initiate multipart upload
3. Backend → Client: Return presigned URLs for each part
4. Client → S3: Upload parts directly (parallel, up to 100)
5. Client → Backend: Notify completion with ETags
6. Backend → S3: Complete multipart upload
7. Backend → DB: Update metadata
8. Backend → EventBridge: Emit PhotoUploadCompleted event
```

**Benefits**:

- Offloads bandwidth from backend servers
- Enables true parallel uploads (100 concurrent)
- Reduces server load and cost
- Client retains control over progress tracking
- Backend validates and orchestrates without handling bytes


#### **1.2.2 Concurrency Model**

**Backend**:

- Spring Boot with Virtual Threads (Java 21+) or `@Async` with custom thread pool
- `ThreadPoolTaskExecutor` configured for 100+ concurrent operations
- Non-blocking I/O for S3 operations using AWS SDK v2 async client

**Frontend**:

- Web: `Promise.all()` with batched concurrency control (10-20 simultaneous uploads)
- Mobile: Background upload service with queue management


#### **1.2.3 CQRS Implementation**

**Command Side** (Writes):

- `InitiateUploadCommand` → Generates presigned URLs
- `CompleteUploadCommand` → Finalizes multipart upload
- `UpdatePhotoMetadataCommand` → Tags, descriptions

**Query Side** (Reads):

- `GetPhotosQuery` → List user photos with metadata
- `GetUploadStatusQuery` → Real-time upload progress
- Separate read models optimized for queries (denormalized views)

***

## 2. Database Schema (PostgreSQL)

### 2.1 Entity-Relationship Diagram

```
┌─────────────────────┐
│       users         │
├─────────────────────┤
│ id (PK)            │
│ email              │
│ password_hash      │
│ created_at         │
│ updated_at         │
└──────────┬──────────┘
           │
           │ 1:N
           │
┌──────────▼──────────┐
│  upload_sessions    │
├─────────────────────┤
│ id (PK)            │
│ user_id (FK)       │
│ session_token      │
│ total_photos       │
│ completed_photos   │
│ failed_photos      │
│ status             │
│ started_at         │
│ completed_at       │
└──────────┬──────────┘
           │
           │ 1:N
           │
┌──────────▼──────────────────┐
│         photos              │
├─────────────────────────────┤
│ id (PK)                    │
│ user_id (FK)               │
│ upload_session_id (FK)     │
│ s3_key                     │
│ s3_bucket                  │
│ s3_etag                    │
│ original_filename          │
│ file_size_bytes            │
│ mime_type                  │
│ upload_status              │ (ENUM: INITIATED, UPLOADING, COMPLETED, FAILED)
│ multipart_upload_id        │
│ tags                       │ (JSONB)
│ metadata                   │ (JSONB: dimensions, camera info, etc.)
│ thumbnail_url              │
│ created_at                 │
│ updated_at                 │
└─────────────────────────────┘

┌─────────────────────────────┐
│    upload_parts             │
├─────────────────────────────┤
│ id (PK)                    │
│ photo_id (FK)              │
│ part_number                │
│ etag                       │
│ size_bytes                 │
│ uploaded_at                │
└─────────────────────────────┘

┌─────────────────────────────┐
│  photo_events (Event Store) │
├─────────────────────────────┤
│ id (PK)                    │
│ photo_id (FK)              │
│ event_type                 │
│ event_data (JSONB)         │
│ occurred_at                │
└─────────────────────────────┘
```


### 2.2 SQL Schema Definition

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Upload sessions table
CREATE TABLE upload_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    total_photos INT DEFAULT 0,
    completed_photos INT DEFAULT 0,
    failed_photos INT DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_session_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_upload_sessions_user_id ON upload_sessions(user_id);
CREATE INDEX idx_upload_sessions_status ON upload_sessions(status);

-- Photos table
CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    upload_session_id UUID REFERENCES upload_sessions(id) ON DELETE SET NULL,
    s3_key VARCHAR(1024) NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_etag VARCHAR(255),
    original_filename VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    upload_status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    multipart_upload_id VARCHAR(255),
    tags JSONB DEFAULT '[]'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb,
    thumbnail_url VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_upload_status CHECK (upload_status IN ('INITIATED', 'UPLOADING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_photos_user_id ON photos(user_id);
CREATE INDEX idx_photos_upload_session_id ON photos(upload_session_id);
CREATE INDEX idx_photos_upload_status ON photos(upload_status);
CREATE INDEX idx_photos_created_at ON photos(created_at DESC);
CREATE INDEX idx_photos_tags ON photos USING GIN (tags);

-- Upload parts table (for tracking multipart uploads)
CREATE TABLE upload_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photo_id UUID NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    part_number INT NOT NULL,
    etag VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(photo_id, part_number)
);

CREATE INDEX idx_upload_parts_photo_id ON upload_parts(photo_id);

-- Photo events table (for event sourcing)
CREATE TABLE photo_events (
    id BIGSERIAL PRIMARY KEY,
    photo_id UUID NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_photo_events_photo_id ON photo_events(photo_id);
CREATE INDEX idx_photo_events_occurred_at ON photo_events(occurred_at DESC);
```


***

## 3. AWS Infrastructure Architecture

### 3.1 AWS Services Breakdown

| Service | Purpose | Configuration |
| :-- | :-- | :-- |
| **ECS Fargate** | Spring Boot application hosting | Auto-scaling group, 2-10 tasks |
| **Application Load Balancer** | Traffic distribution, SSL termination | Target group for ECS tasks |
| **RDS PostgreSQL** | Metadata persistence | Multi-AZ, t3.medium (production: r6g.xlarge) |
| **S3** | Photo storage | Versioning enabled, lifecycle policies |
| **CloudFront** | CDN for photo delivery | Origin: S3 bucket |
| **EventBridge** | Event routing | Rules for S3 events → SQS |
| **SQS** | Async post-processing queue | Standard queue, dead-letter queue |
| **Lambda** | Thumbnail generation | Triggered by EventBridge |
| **Secrets Manager** | Credentials storage | DB passwords, API keys |
| **CloudWatch** | Monitoring \& logging | Custom metrics, alarms |
| **VPC** | Network isolation | Public/private subnets |

### 3.2 Infrastructure Diagram

```
┌────────────────────────────────────────────────────────────────────┐
│                              AWS Cloud                              │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                      VPC (10.0.0.0/16)                        │ │
│  │                                                                │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐            │ │
│  │  │  Public Subnet 1a   │  │  Public Subnet 1b   │            │ │
│  │  │  ┌───────────────┐  │  │  ┌───────────────┐  │            │ │
│  │  │  │      ALB      │◄─┼──┼──┤      ALB      │  │            │ │
│  │  │  └───────┬───────┘  │  │  └───────────────┘  │            │ │
│  │  └──────────┼──────────┘  └─────────────────────┘            │ │
│  │             │                                                  │ │
│  │  ┌──────────▼──────────┐  ┌─────────────────────┐            │ │
│  │  │  Private Subnet 1a  │  │  Private Subnet 1b  │            │ │
│  │  │  ┌───────────────┐  │  │  ┌───────────────┐  │            │ │
│  │  │  │  ECS Fargate  │  │  │  │  ECS Fargate  │  │            │ │
│  │  │  │   (Task 1)    │  │  │  │   (Task 2)    │  │            │ │
│  │  │  └───────┬───────┘  │  │  └───────┬───────┘  │            │ │
│  │  └──────────┼──────────┘  └──────────┼──────────┘            │ │
│  │             │                         │                        │ │
│  │  ┌──────────▼─────────────────────────▼──────────┐            │ │
│  │  │        RDS PostgreSQL (Multi-AZ)               │            │ │
│  │  │        Primary + Standby                       │            │ │
│  │  └────────────────────────────────────────────────┘            │ │
│  │                                                                │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                         S3 Bucket                             │ │
│  │  - rapid-photo-upload-production                              │ │
│  │  - Versioning: Enabled                                        │ │
│  │  - Lifecycle: Intelligent-Tiering                             │ │
│  │  - Event Notifications → EventBridge                          │ │
│  └─────────────────────────┬────────────────────────────────────┘ │
│                            │                                       │
│  ┌─────────────────────────▼────────────────────────────────────┐ │
│  │                    CloudFront Distribution                    │ │
│  │  - Origin: S3 bucket                                          │ │
│  │  - OAI (Origin Access Identity) for security                 │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │              EventBridge → SQS → Lambda                       │ │
│  │  Event: s3:ObjectCreated:CompleteMultipartUpload              │ │
│  │  → SQS Queue → Lambda (Thumbnail Generation)                 │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```


### 3.3 Infrastructure as Code (Terraform Example)

```hcl
# Key resources definition
resource "aws_s3_bucket" "photo_storage" {
  bucket = "rapid-photo-upload-${var.environment}"
  
  lifecycle_rule {
    enabled = true
    transition {
      days          = 90
      storage_class = "INTELLIGENT_TIERING"
    }
  }
}

resource "aws_ecs_cluster" "main" {
  name = "rapid-photo-upload-cluster"
}

resource "aws_ecs_service" "spring_boot" {
  name            = "photo-upload-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 2
  launch_type     = "FARGATE"
  
  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "spring-boot-app"
    container_port   = 8080
  }
  
  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

resource "aws_db_instance" "postgres" {
  identifier        = "rapid-photo-upload-db"
  engine            = "postgres"
  engine_version    = "15.4"
  instance_class    = "db.t3.medium"
  allocated_storage = 100
  multi_az          = true
  
  db_name  = "photoupload"
  username = "dbadmin"
  password = random_password.db_password.result
}
```


***

## 4. Backend Architecture (Spring Boot)

### 4.1 Package Structure (Vertical Slice Architecture)

```
src/main/java/com/rapidphotoupload/
│
├── domain/                              # Domain Layer (DDD)
│   ├── model/
│   │   ├── Photo.java                  # Aggregate Root
│   │   ├── UploadSession.java          # Aggregate Root
│   │   ├── User.java                   # Aggregate Root
│   │   ├── PhotoMetadata.java          # Value Object
│   │   ├── UploadStatus.java           # Enum
│   │   └── UploadPart.java             # Entity
│   ├── event/
│   │   ├── PhotoUploadInitiated.java
│   │   ├── PhotoUploadCompleted.java
│   │   └── PhotoUploadFailed.java
│   └── repository/
│       ├── PhotoRepository.java
│       ├── UploadSessionRepository.java
│       └── UserRepository.java
│
├── application/                         # Application Layer (CQRS)
│   ├── command/
│   │   ├── InitiateUploadCommand.java
│   │   ├── CompleteUploadCommand.java
│   │   └── UpdatePhotoMetadataCommand.java
│   ├── query/
│   │   ├── GetPhotosQuery.java
│   │   ├── GetUploadStatusQuery.java
│   │   └── GetPhotoByIdQuery.java
│   ├── handler/
│   │   ├── InitiateUploadHandler.java
│   │   ├── CompleteUploadHandler.java
│   │   ├── GetPhotosHandler.java
│   │   └── GetUploadStatusHandler.java
│   └── dto/
│       ├── UploadInitiationResponse.java
│       ├── PhotoDto.java
│       └── UploadStatusDto.java
│
├── feature/                             # Vertical Slices
│   ├── upload/
│   │   ├── UploadController.java
│   │   ├── UploadService.java
│   │   └── UploadSliceConfiguration.java
│   ├── photo/
│   │   ├── PhotoController.java
│   │   ├── PhotoService.java
│   │   └── PhotoSliceConfiguration.java
│   └── auth/
│       ├── AuthController.java
│       ├── AuthService.java
│       └── AuthSliceConfiguration.java
│
├── infrastructure/                      # Infrastructure Layer
│   ├── persistence/
│   │   ├── JpaPhotoRepository.java
│   │   ├── JpaUploadSessionRepository.java
│   │   └── entity/
│   │       ├── PhotoEntity.java
│   │       └── UploadSessionEntity.java
│   ├── storage/
│   │   ├── S3StorageService.java
│   │   └── PresignedUrlGenerator.java
│   ├── event/
│   │   ├── EventBridgePublisher.java
│   │   └── DomainEventPublisher.java
│   └── config/
│       ├── AwsConfig.java
│       ├── AsyncConfig.java
│       ├── SecurityConfig.java
│       └── DatabaseConfig.java
│
└── shared/                              # Shared Kernel
    ├── exception/
    │   ├── PhotoNotFoundException.java
    │   └── UploadException.java
    └── util/
        ├── IdGenerator.java
        └── FileValidator.java
```


### 4.2 Key Code Examples

#### **4.2.1 Domain Model: Photo Aggregate**

```java
@Entity
@Table(name = "photos")
public class Photo {
    @Id
    private UUID id;
    
    private UUID userId;
    
    @Embedded
    private PhotoMetadata metadata;
    
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;
    
    private String multipartUploadId;
    
    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL)
    private List<UploadPart> parts = new ArrayList<>();
    
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    // Business logic
    public void initiateUpload(String multipartUploadId) {
        this.multipartUploadId = multipartUploadId;
        this.uploadStatus = UploadStatus.INITIATED;
        this.addDomainEvent(new PhotoUploadInitiated(this.id, this.userId));
    }
    
    public void markPartUploaded(int partNumber, String etag, long sizeBytes) {
        UploadPart part = new UploadPart(partNumber, etag, sizeBytes);
        this.parts.add(part);
        this.uploadStatus = UploadStatus.UPLOADING;
    }
    
    public void completeUpload(String etag) {
        this.metadata.setS3Etag(etag);
        this.uploadStatus = UploadStatus.COMPLETED;
        this.addDomainEvent(new PhotoUploadCompleted(this.id, this.userId));
    }
    
    public void failUpload(String reason) {
        this.uploadStatus = UploadStatus.FAILED;
        this.addDomainEvent(new PhotoUploadFailed(this.id, this.userId, reason));
    }
    
    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
```


#### **4.2.2 Command: InitiateUploadCommand**

```java
public record InitiateUploadCommand(
    UUID userId,
    String filename,
    long fileSizeBytes,
    String mimeType,
    UUID uploadSessionId
) {}
```


#### **4.2.3 Command Handler: InitiateUploadHandler**

```java
@Service
@RequiredArgsConstructor
public class InitiateUploadHandler {
    
    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;
    private final DomainEventPublisher eventPublisher;
    
    @Transactional
    public UploadInitiationResponse handle(InitiateUploadCommand command) {
        // Create photo aggregate
        Photo photo = new Photo(
            UUID.randomUUID(),
            command.userId(),
            command.filename(),
            command.fileSizeBytes(),
            command.mimeType()
        );
        
        // Generate S3 key
        String s3Key = generateS3Key(command.userId(), photo.getId(), command.filename());
        
        // Initiate multipart upload with S3
        String uploadId = s3StorageService.initiateMultipartUpload(s3Key);
        
        // Update domain model
        photo.initiateUpload(uploadId);
        
        // Calculate number of parts (5MB chunks)
        int numberOfParts = (int) Math.ceil(command.fileSizeBytes() / (5.0 * 1024 * 1024));
        
        // Generate presigned URLs for each part
        List<PresignedUrl> presignedUrls = s3StorageService.generatePresignedUrls(
            s3Key, 
            uploadId, 
            numberOfParts
        );
        
        // Save aggregate
        photoRepository.save(photo);
        
        // Publish domain events
        eventPublisher.publish(photo.getDomainEvents());
        photo.clearDomainEvents();
        
        return new UploadInitiationResponse(
            photo.getId(),
            uploadId,
            s3Key,
            presignedUrls
        );
    }
    
    private String generateS3Key(UUID userId, UUID photoId, String filename) {
        String timestamp = Instant.now().toString();
        String sanitizedFilename = sanitizeFilename(filename);
        return String.format("uploads/%s/%s/%s", userId, photoId, sanitizedFilename);
    }
}
```


#### **4.2.4 Vertical Slice: UploadController**

```java
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {
    
    private final InitiateUploadHandler initiateUploadHandler;
    private final CompleteUploadHandler completeUploadHandler;
    
    @PostMapping("/initiate")
    public ResponseEntity<UploadInitiationResponse> initiateUpload(
        @Valid @RequestBody InitiateUploadRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        InitiateUploadCommand command = new InitiateUploadCommand(
            user.getUserId(),
            request.getFilename(),
            request.getFileSizeBytes(),
            request.getMimeType(),
            request.getUploadSessionId()
        );
        
        UploadInitiationResponse response = initiateUploadHandler.handle(command);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{photoId}/complete")
    public ResponseEntity<Void> completeUpload(
        @PathVariable UUID photoId,
        @Valid @RequestBody CompleteUploadRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        CompleteUploadCommand command = new CompleteUploadCommand(
            photoId,
            user.getUserId(),
            request.getUploadId(),
            request.getParts()
        );
        
        completeUploadHandler.handle(command);
        return ResponseEntity.ok().build();
    }
}
```


#### **4.2.5 Infrastructure: S3StorageService**

```java
@Service
@RequiredArgsConstructor
public class S3StorageService {
    
    private final S3AsyncClient s3AsyncClient;
    
    @Value("${aws.s3.bucket.name}")
    private String bucketName;
    
    public String initiateMultipartUpload(String key) {
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();
        
        try {
            CreateMultipartUploadResponse response = s3AsyncClient
                .createMultipartUpload(request)
                .get(10, TimeUnit.SECONDS);
            return response.uploadId();
        } catch (Exception e) {
            throw new UploadException("Failed to initiate multipart upload", e);
        }
    }
    
    public List<PresignedUrl> generatePresignedUrls(
        String key, 
        String uploadId, 
        int numberOfParts
    ) {
        List<PresignedUrl> presignedUrls = new ArrayList<>();
        
        for (int partNumber = 1; partNumber <= numberOfParts; partNumber++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();
            
            PresignedUploadPartRequest presignedRequest = 
                PresignedUploadPartRequest.builder()
                    .signatureDuration(Duration.ofHours(2))
                    .uploadPartRequest(uploadPartRequest)
                    .build();
            
            PresignedUploadPartRequest presigned = 
                s3Presigner.presignUploadPart(presignedRequest);
            
            presignedUrls.add(new PresignedUrl(
                partNumber,
                presigned.url().toString(),
                presigned.expiration()
            ));
        }
        
        return presignedUrls;
    }
    
    @Async
    public CompletableFuture<String> completeMultipartUpload(
        String key,
        String uploadId,
        List<CompletedPart> parts
    ) {
        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(key)
            .uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder()
                .parts(parts)
                .build())
            .build();
        
        return s3AsyncClient.completeMultipartUpload(request)
            .thenApply(CompleteMultipartUploadResponse::eTag);
    }
}
```


#### **4.2.6 Async Configuration**

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "uploadTaskExecutor")
    public ThreadPoolTaskExecutor uploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("upload-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```


***

## 5. Frontend Architecture

### 5.1 React Web Application

#### **5.1.1 Project Structure**

```
src/
├── features/
│   ├── upload/
│   │   ├── components/
│   │   │   ├── UploadZone.tsx
│   │   │   ├── UploadProgressList.tsx
│   │   │   └── UploadProgressItem.tsx
│   │   ├── hooks/
│   │   │   ├── useUploadManager.ts
│   │   │   └── useUploadProgress.ts
│   │   ├── services/
│   │   │   ├── uploadService.ts
│   │   │   └── uploadQueue.ts
│   │   └── types/
│   │       └── upload.types.ts
│   ├── gallery/
│   │   ├── components/
│   │   │   ├── PhotoGrid.tsx
│   │   │   └── PhotoCard.tsx
│   │   └── hooks/
│   │       └── usePhotos.ts
│   └── auth/
│       ├── components/
│       │   └── LoginForm.tsx
│       └── hooks/
│           └── useAuth.ts
├── shared/
│   ├── api/
│   │   ├── apiClient.ts
│   │   └── endpoints.ts
│   ├── hooks/
│   │   └── useAsync.ts
│   └── utils/
│       └── fileUtils.ts
└── App.tsx
```


#### **5.1.2 Upload Manager Hook**

```typescript
// useUploadManager.ts
import { useState, useCallback, useRef } from 'react';
import { uploadService } from '../services/uploadService';

interface UploadItem {
  id: string;
  file: File;
  status: 'pending' | 'uploading' | 'completed' | 'failed';
  progress: number;
  error?: string;
}

export const useUploadManager = (concurrency: number = 10) => {
  const [uploads, setUploads] = useState<Map<string, UploadItem>>(new Map());
  const [activeUploads, setActiveUploads] = useState(0);
  const queueRef = useRef<string[]>([]);
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());

  const addFiles = useCallback((files: File[]) => {
    const newUploads = new Map(uploads);
    
    files.forEach(file => {
      const id = crypto.randomUUID();
      newUploads.set(id, {
        id,
        file,
        status: 'pending',
        progress: 0
      });
      queueRef.current.push(id);
    });
    
    setUploads(newUploads);
    processQueue();
  }, [uploads]);

  const processQueue = useCallback(async () => {
    while (queueRef.current.length > 0 && activeUploads < concurrency) {
      const uploadId = queueRef.current.shift();
      if (!uploadId) continue;

      const uploadItem = uploads.get(uploadId);
      if (!uploadItem) continue;

      setActiveUploads(prev => prev + 1);
      
      const abortController = new AbortController();
      abortControllersRef.current.set(uploadId, abortController);

      updateUploadStatus(uploadId, 'uploading');

      try {
        await uploadService.uploadFile(
          uploadItem.file,
          {
            onProgress: (progress) => {
              updateUploadProgress(uploadId, progress);
            },
            signal: abortController.signal
          }
        );
        
        updateUploadStatus(uploadId, 'completed');
      } catch (error) {
        updateUploadStatus(uploadId, 'failed', error.message);
      } finally {
        setActiveUploads(prev => prev - 1);
        abortControllersRef.current.delete(uploadId);
        processQueue(); // Continue with next in queue
      }
    }
  }, [uploads, activeUploads, concurrency]);

  const updateUploadStatus = useCallback(
    (id: string, status: UploadItem['status'], error?: string) => {
      setUploads(prev => {
        const newUploads = new Map(prev);
        const upload = newUploads.get(id);
        if (upload) {
          newUploads.set(id, { ...upload, status, error });
        }
        return newUploads;
      });
    },
    []
  );

  const updateUploadProgress = useCallback((id: string, progress: number) => {
    setUploads(prev => {
      const newUploads = new Map(prev);
      const upload = newUploads.get(id);
      if (upload) {
        newUploads.set(id, { ...upload, progress });
      }
      return newUploads;
    });
  }, []);

  const cancelUpload = useCallback((id: string) => {
    const controller = abortControllersRef.current.get(id);
    controller?.abort();
    updateUploadStatus(id, 'failed', 'Upload cancelled');
  }, []);

  return {
    uploads: Array.from(uploads.values()),
    addFiles,
    cancelUpload,
    activeUploads,
    totalUploads: uploads.size
  };
};
```


#### **5.1.3 Upload Service**

```typescript
// uploadService.ts
import axios from 'axios';

interface UploadOptions {
  onProgress?: (progress: number) => void;
  signal?: AbortSignal;
}

interface PresignedUrlResponse {
  photoId: string;
  uploadId: string;
  s3Key: string;
  presignedUrls: Array<{
    partNumber: number;
    url: string;
    expiration: string;
  }>;
}

const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB

export const uploadService = {
  async uploadFile(file: File, options: UploadOptions = {}) {
    // Step 1: Initiate upload
    const initResponse = await axios.post<PresignedUrlResponse>(
      '/api/v1/uploads/initiate',
      {
        filename: file.name,
        fileSizeBytes: file.size,
        mimeType: file.type
      },
      { signal: options.signal }
    );

    const { photoId, uploadId, presignedUrls } = initResponse.data;

    // Step 2: Upload parts in parallel
    const uploadedParts = await this.uploadParts(
      file,
      presignedUrls,
      options
    );

    // Step 3: Complete upload
    await axios.post(
      `/api/v1/uploads/${photoId}/complete`,
      {
        uploadId,
        parts: uploadedParts
      },
      { signal: options.signal }
    );

    return photoId;
  },

  async uploadParts(
    file: File,
    presignedUrls: PresignedUrlResponse['presignedUrls'],
    options: UploadOptions
  ) {
    const chunks = this.splitFile(file, CHUNK_SIZE);
    const uploadPromises = presignedUrls.map(async (presigned, index) => {
      const chunk = chunks[index];
      
      const response = await fetch(presigned.url, {
        method: 'PUT',
        body: chunk,
        headers: {
          'Content-Type': file.type
        },
        signal: options.signal
      });

      if (!response.ok) {
        throw new Error(`Part ${presigned.partNumber} upload failed`);
      }

      const etag = response.headers.get('ETag')?.replace(/"/g, '');
      
      // Update progress
      if (options.onProgress) {
        const progress = ((index + 1) / presignedUrls.length) * 100;
        options.onProgress(progress);
      }

      return {
        partNumber: presigned.partNumber,
        etag
      };
    });

    return await Promise.all(uploadPromises);
  },

  splitFile(file: File, chunkSize: number): Blob[] {
    const chunks: Blob[] = [];
    let offset = 0;

    while (offset < file.size) {
      const chunk = file.slice(offset, offset + chunkSize);
      chunks.push(chunk);
      offset += chunkSize;
    }

    return chunks;
  }
};
```


### 5.2 React Native Mobile Application

#### **5.2.1 Background Upload Service**

```typescript
// backgroundUploadService.ts
import * as BackgroundFetch from 'expo-background-fetch';
import * as TaskManager from 'expo-task-manager';
import AsyncStorage from '@react-native-async-storage/async-storage';

const UPLOAD_TASK_NAME = 'background-photo-upload';
const UPLOAD_QUEUE_KEY = 'upload_queue';

interface QueuedUpload {
  id: string;
  uri: string;
  filename: string;
  fileSize: number;
  mimeType: string;
  status: 'queued' | 'uploading' | 'completed' | 'failed';
}

// Register background task
TaskManager.defineTask(UPLOAD_TASK_NAME, async () => {
  try {
    const queue = await getUploadQueue();
    const pendingUploads = queue.filter(u => u.status === 'queued');
    
    for (const upload of pendingUploads) {
      await processUpload(upload);
    }
    
    return BackgroundFetch.BackgroundFetchResult.NewData;
  } catch (error) {
    return BackgroundFetch.BackgroundFetchResult.Failed;
  }
});

export const backgroundUploadService = {
  async registerBackgroundTask() {
    await BackgroundFetch.registerTaskAsync(UPLOAD_TASK_NAME, {
      minimumInterval: 15 * 60, // 15 minutes
      stopOnTerminate: false,
      startOnBoot: true
    });
  },

  async queueUpload(uri: string, filename: string, fileSize: number) {
    const queue = await getUploadQueue();
    const newUpload: QueuedUpload = {
      id: Date.now().toString(),
      uri,
      filename,
      fileSize,
      mimeType: 'image/jpeg',
      status: 'queued'
    };
    
    queue.push(newUpload);
    await AsyncStorage.setItem(UPLOAD_QUEUE_KEY, JSON.stringify(queue));
    
    // Trigger immediate upload if possible
    await processUpload(newUpload);
    
    return newUpload.id;
  },

  async getUploadQueue(): Promise<QueuedUpload[]> {
    const queueJson = await AsyncStorage.getItem(UPLOAD_QUEUE_KEY);
    return queueJson ? JSON.parse(queueJson) : [];
  }
};

async function processUpload(upload: QueuedUpload) {
  // Implementation similar to web version
  // Using react-native-fs for file reading
}
```


***

## 6. Epic \& Story Breakdown

### Epic 1: Foundation \& Infrastructure Setup

**Goal**: Establish AWS infrastructure, database, and core backend framework

#### Story 1.1: AWS Infrastructure Provisioning

- **Tasks**:
    - Create VPC with public/private subnets
    - Set up RDS PostgreSQL Multi-AZ
    - Create S3 bucket with lifecycle policies
    - Configure ALB and target groups
    - Set up CloudFront distribution
    - Configure EventBridge and SQS
- **Acceptance Criteria**:
    - All AWS resources provisioned via Terraform
    - RDS accessible from ECS tasks
    - S3 bucket configured with versioning
- **Estimate**: 5 points


#### Story 1.2: Database Schema Implementation

- **Tasks**:
    - Create migration scripts for all tables
    - Add indexes for performance
    - Set up connection pooling
    - Configure database monitoring
- **Acceptance Criteria**:
    - All tables created with constraints
    - Indexes validated for query patterns
    - Connection pool configured (min: 10, max: 50)
- **Estimate**: 3 points


#### Story 1.3: Spring Boot Project Setup

- **Tasks**:
    - Initialize Spring Boot project (Java 21)
    - Configure multi-module structure
    - Set up dependency injection
    - Configure logging (structured JSON)
    - Add AWS SDK dependencies
- **Acceptance Criteria**:
    - Application starts successfully
    - Health check endpoint responds
    - Logs output in JSON format
- **Estimate**: 2 points

***

### Epic 2: Domain Model \& CQRS Implementation

**Goal**: Implement core domain models, aggregates, and CQRS pattern

#### Story 2.1: Domain Model - Photo Aggregate

- **Tasks**:
    - Implement Photo aggregate root
    - Create UploadStatus enum
    - Implement PhotoMetadata value object
    - Add domain events (PhotoUploadInitiated, etc.)
    - Create UploadPart entity
- **Acceptance Criteria**:
    - Photo aggregate enforces business rules
    - Domain events published on state changes
    - Unit tests for business logic (>80% coverage)
- **Estimate**: 5 points


#### Story 2.2: Domain Model - UploadSession Aggregate

- **Tasks**:
    - Implement UploadSession aggregate
    - Track session-level statistics
    - Handle session completion logic
- **Acceptance Criteria**:
    - Session correctly tracks multiple photos
    - Statistics calculated accurately
    - Unit tests passing
- **Estimate**: 3 points


#### Story 2.3: Repository Implementation

- **Tasks**:
    - Create JPA repositories for Photo, UploadSession
    - Implement custom query methods
    - Add pagination support
    - Configure entity mappings
- **Acceptance Criteria**:
    - CRUD operations working
    - Custom queries optimized
    - Integration tests passing
- **Estimate**: 3 points


#### Story 2.4: CQRS Commands \& Handlers

- **Tasks**:
    - Implement InitiateUploadCommand/Handler
    - Implement CompleteUploadCommand/Handler
    - Implement UpdatePhotoMetadataCommand/Handler
    - Add command validation
- **Acceptance Criteria**:
    - Commands immutable (records)
    - Handlers transactional
    - Validation errors thrown appropriately
- **Estimate**: 5 points


#### Story 2.5: CQRS Queries \& Handlers

- **Tasks**:
    - Implement GetPhotosQuery/Handler
    - Implement GetUploadStatusQuery/Handler
    - Optimize read models
    - Add caching for frequent queries
- **Acceptance Criteria**:
    - Queries return DTOs (not domain entities)
    - Response times <100ms
    - Cache hit rate >70%
- **Estimate**: 4 points

***

### Epic 3: S3 Integration \& Multipart Upload

**Goal**: Implement direct-to-S3 upload with presigned URLs

#### Story 3.1: S3 Client Configuration

- **Tasks**:
    - Configure AWS S3 async client
    - Set up credentials via IAM roles
    - Configure retry policies
    - Add circuit breaker (Resilience4j)
- **Acceptance Criteria**:
    - S3 client connects successfully
    - Credentials rotate automatically
    - Circuit breaker trips on failures
- **Estimate**: 3 points


#### Story 3.2: Presigned URL Generation

- **Tasks**:
    - Implement multipart upload initiation
    - Generate presigned URLs for each part
    - Set expiration policy (2 hours)
    - Add URL signing validation
- **Acceptance Criteria**:
    - Presigned URLs generated for 100 parts
    - URLs expire after 2 hours
    - URLs only work for specified parts
- **Estimate**: 5 points


#### Story 3.3: Multipart Upload Completion

- **Tasks**:
    - Implement CompleteMultipartUpload call
    - Validate ETags from client
    - Handle completion failures
    - Abort abandoned uploads (lifecycle policy)
- **Acceptance Criteria**:
    - Upload completes successfully with valid ETags
    - Invalid ETags rejected
    - Abandoned uploads cleaned up after 24 hours
- **Estimate**: 4 points


#### Story 3.4: Upload Error Handling

- **Tasks**:
    - Implement retry logic with exponential backoff
    - Handle network failures
    - Provide detailed error messages
    - Log failures to CloudWatch
- **Acceptance Criteria**:
    - Transient failures retried (max 3 attempts)
    - Permanent failures reported to client
    - All errors logged with context
- **Estimate**: 3 points

***

### Epic 4: Asynchronous Processing \& Concurrency

**Goal**: Handle 100 concurrent uploads with async processing

#### Story 4.1: Async Configuration

- **Tasks**:
    - Configure ThreadPoolTaskExecutor
    - Set core/max pool sizes (50/100)
    - Configure queue capacity (500)
    - Add thread pool monitoring
- **Acceptance Criteria**:
    - 100 uploads processed concurrently
    - No thread starvation
    - Metrics exposed for monitoring
- **Estimate**: 3 points


#### Story 4.2: Event-Driven Architecture

- **Tasks**:
    - Publish domain events to EventBridge
    - Configure EventBridge rules
    - Set up SQS queue for post-processing
    - Implement dead-letter queue
- **Acceptance Criteria**:
    - Events published on upload completion
    - SQS receives events within 1 second
    - Failed events moved to DLQ
- **Estimate**: 5 points


#### Story 4.3: Load Testing \& Performance Tuning

- **Tasks**:
    - Create JMeter test plan (100 concurrent uploads)
    - Run load tests
    - Profile application (JVisualVM)
    - Optimize bottlenecks
- **Acceptance Criteria**:
    - 100 photos (200MB total) uploaded in <90 seconds
    - CPU usage <80%
    - Memory stable (no leaks)
- **Estimate**: 5 points

***

### Epic 5: Authentication \& Security

**Goal**: Secure API with JWT authentication

#### Story 5.1: JWT Authentication

- **Tasks**:
    - Implement JWT token generation
    - Create login endpoint
    - Add Spring Security configuration
    - Implement user authentication
- **Acceptance Criteria**:
    - JWT tokens issued on login
    - Tokens expire after 24 hours
    - Invalid tokens rejected (401)
- **Estimate**: 4 points


#### Story 5.2: Authorization

- **Tasks**:
    - Add user context to requests
    - Implement authorization checks
    - Ensure users only access their photos
- **Acceptance Criteria**:
    - Users cannot access other users' photos
    - Unauthorized requests return 403
- **Estimate**: 3 points


#### Story 5.3: Security Hardening

- **Tasks**:
    - Add CORS configuration
    - Implement rate limiting
    - Add input validation
    - Configure HTTPS
- **Acceptance Criteria**:
    - CORS configured for frontend domains
    - Rate limiting prevents abuse (100 req/min)
    - All inputs validated
- **Estimate**: 3 points

***

### Epic 6: React Web Application

**Goal**: Build responsive web interface for photo upload and viewing

#### Story 6.1: Project Setup

- **Tasks**:
    - Initialize React + TypeScript project
    - Set up build pipeline (Vite)
    - Configure ESLint + Prettier
    - Add TailwindCSS
- **Acceptance Criteria**:
    - Application runs on localhost
    - Build process successful
    - Linting enforced
- **Estimate**: 2 points


#### Story 6.2: Upload UI Components

- **Tasks**:
    - Create UploadZone (drag \& drop)
    - Build UploadProgressList
    - Implement UploadProgressItem
    - Add file validation (type, size)
- **Acceptance Criteria**:
    - Drag \& drop works
    - Progress bars update in real-time
    - Invalid files rejected with message
- **Estimate**: 5 points


#### Story 6.3: Upload Manager Hook

- **Tasks**:
    - Implement useUploadManager
    - Add concurrency control (10 simultaneous)
    - Track upload states
    - Handle cancellations
- **Acceptance Criteria**:
    - 100 files uploaded successfully
    - UI remains responsive
    - Uploads can be cancelled
- **Estimate**: 5 points


#### Story 6.4: Photo Gallery

- **Tasks**:
    - Build PhotoGrid component
    - Implement lazy loading
    - Add photo viewer modal
    - Enable tagging and metadata editing
- **Acceptance Criteria**:
    - Grid displays all photos
    - Images load on scroll
    - Modal shows full-size image
    - Tags can be added/removed
- **Estimate**: 5 points


#### Story 6.5: Authentication Flow

- **Tasks**:
    - Create LoginForm component
    - Implement useAuth hook
    - Add protected routes
    - Handle token refresh
- **Acceptance Criteria**:
    - Users can log in
    - Protected routes redirect to login
    - Tokens stored securely (httpOnly cookies)
- **Estimate**: 4 points

***

### Epic 7: React Native Mobile Application

**Goal**: Build mobile app with background upload capability

#### Story 7.1: Project Setup

- **Tasks**:
    - Initialize React Native project (Expo)
    - Configure TypeScript
    - Add navigation (React Navigation)
    - Set up AsyncStorage
- **Acceptance Criteria**:
    - App runs on iOS simulator
    - App runs on Android emulator
    - Navigation works
- **Estimate**: 3 points


#### Story 7.2: Photo Selection \& Upload

- **Tasks**:
    - Implement photo picker (expo-image-picker)
    - Build upload queue UI
    - Add progress tracking
    - Handle permissions (camera, photos)
- **Acceptance Criteria**:
    - Users can select multiple photos
    - Upload progress displayed
    - Permissions requested appropriately
- **Estimate**: 5 points


#### Story 7.3: Background Upload Service

- **Tasks**:
    - Implement BackgroundFetch integration
    - Create upload queue persistence
    - Handle app backgrounding
    - Add retry logic
- **Acceptance Criteria**:
    - Uploads continue when app backgrounded
    - Failed uploads retried on next launch
    - Queue persists across app restarts
- **Estimate**: 8 points


#### Story 7.4: Photo Gallery (Mobile)

- **Tasks**:
    - Build FlatList photo grid
    - Add pull-to-refresh
    - Implement photo viewer
    - Enable offline caching
- **Acceptance Criteria**:
    - Grid performs smoothly (60fps)
    - Photos cached locally
    - Refresh updates from server
- **Estimate**: 5 points

***

### Epic 8: Post-Processing \& Thumbnails

**Goal**: Generate thumbnails for uploaded photos

#### Story 8.1: Lambda Thumbnail Generator

- **Tasks**:
    - Create Lambda function (Python/Node.js)
    - Integrate Sharp/Pillow for resizing
    - Generate 3 sizes (thumbnail, medium, large)
    - Upload thumbnails to S3
- **Acceptance Criteria**:
    - Thumbnails generated within 5 seconds
    - Images resized correctly
    - Thumbnails stored in separate S3 prefix
- **Estimate**: 5 points


#### Story 8.2: Event-Driven Thumbnail Trigger

- **Tasks**:
    - Configure EventBridge rule for S3 events
    - Route to SQS queue
    - Trigger Lambda from SQS
    - Update database with thumbnail URLs
- **Acceptance Criteria**:
    - Lambda triggered on upload completion
    - Thumbnail URLs saved to database
    - Frontend displays thumbnails
- **Estimate**: 4 points

***

### Epic 9: Testing \& Quality Assurance

**Goal**: Comprehensive testing coverage

#### Story 9.1: Backend Unit Tests

- **Tasks**:
    - Write tests for domain models
    - Test command/query handlers
    - Mock external dependencies
- **Acceptance Criteria**:
    - >80% code coverage
    - All tests passing
- **Estimate**: 5 points


#### Story 9.2: Backend Integration Tests

- **Tasks**:
    - Write end-to-end upload flow tests
    - Use Testcontainers for PostgreSQL
    - Mock S3 with LocalStack
- **Acceptance Criteria**:
    - Complete upload cycle tested
    - Database state validated
    - S3 interactions verified
- **Estimate**: 5 points


#### Story 9.3: Frontend Unit Tests

- **Tasks**:
    - Test React components (Jest, Testing Library)
    - Test custom hooks
    - Mock API calls (MSW)
- **Acceptance Criteria**:
    - Components render correctly
    - User interactions work
    - >70% coverage
- **Estimate**: 4 points


#### Story 9.4: End-to-End Tests

- **Tasks**:
    - Write Cypress tests for upload flow
    - Test authentication
    - Test gallery viewing
- **Acceptance Criteria**:
    - Upload 10 photos successfully
    - Photos appear in gallery
    - Authentication works
- **Estimate**: 5 points

***

### Epic 10: Deployment \& DevOps

**Goal**: Automated deployment pipeline

#### Story 10.1: CI/CD Pipeline

- **Tasks**:
    - Set up GitHub Actions
    - Configure Docker builds
    - Push images to ECR
    - Deploy to ECS
- **Acceptance Criteria**:
    - Pipeline runs on PR merge
    - Images built and pushed
    - ECS tasks updated
- **Estimate**: 5 points


#### Story 10.2: Monitoring \& Observability

- **Tasks**:
    - Configure CloudWatch dashboards
    - Set up alarms (CPU, memory, error rate)
    - Add distributed tracing (X-Ray)
    - Implement structured logging
- **Acceptance Criteria**:
    - Dashboards show key metrics
    - Alarms trigger on thresholds
    - Traces visible in X-Ray
- **Estimate**: 4 points


#### Story 10.3: Documentation

- **Tasks**:
    - Write architecture document (this!)
    - Create API documentation (OpenAPI)
    - Add README with setup instructions
    - Record demo video
- **Acceptance Criteria**:
    - Documentation complete
    - Demo video shows 100 concurrent uploads
- **Estimate**: 3 points

***

## 7. Implementation Timeline (5-Day Sprint)

| Day | Focus Areas | Key Deliverables |
| :-- | :-- | :-- |
| **Day 1** | Infrastructure + Foundation | AWS setup, Database schema, Spring Boot skeleton |
| **Day 2** | Domain Model + CQRS | Photo/UploadSession aggregates, Commands/Queries, S3 integration |
| **Day 3** | Backend Completion + Web Frontend | Complete backend APIs, React upload components |
| **Day 4** | Mobile App + Testing | React Native app, Background uploads, Integration tests |
| **Day 5** | Post-processing + Polish | Thumbnail generation, Load testing, Documentation, Demo |


***

## 8. Key Questions \& Clarifications

Before implementation, please clarify:

1. **Authentication**: Should we use a mock JWT system or integrate with a real provider (Cognito, Auth0)?
2. **File size limits**: What's the maximum file size per photo? (Recommend 50MB max)
3. **Storage costs**: Should we implement intelligent tiering or standard S3 storage?
4. **Mobile platforms**: iOS only, Android only, or both?
5. **AI features**: Priority for AI tagging/categorization? (Rekognition integration)
6. **Observability**: Should we add APM tools (DataDog, New Relic) or stick with CloudWatch?

***

## 9. Success Metrics

| Metric | Target | Measurement |
| :-- | :-- | :-- |
| **Concurrent uploads** | 100 photos | JMeter load test |
| **Upload time (100 photos @ 2MB each)** | <90 seconds | End-to-end timer |
| **UI responsiveness** | <100ms frame time | Chrome DevTools |
| **API response time (P95)** | <200ms | CloudWatch metrics |
| **Code coverage** | >80% backend, >70% frontend | JaCoCo, Jest |
| **Uptime** | 99.9% | CloudWatch alarms |


***

This comprehensive plan provides everything needed to build the RapidPhotoUpload system. The architecture follows DDD/CQRS/VSA principles, leverages AWS services optimally, and delivers a production-grade solution demonstrating mastery of concurrent systems, cloud architecture, and modern full-stack development.

Ready to start implementation? 🚀
<span style="display:none">[^1][^10][^11][^12][^13][^14][^15][^16][^17][^18][^19][^2][^20][^21][^22][^23][^24][^25][^26][^27][^28][^29][^3][^30][^31][^32][^33][^34][^35][^36][^37][^38][^39][^4][^40][^5][^6][^7][^8][^9]</span>

<div align="center">⁂</div>

[^1]: https://aws.amazon.com/blogs/security/how-to-securely-transfer-files-with-presigned-urls/

[^2]: https://stackoverflow.com/questions/78189235/aws-s3-presigned-post-url-multipart-uploads

[^3]: https://dev.to/magpys/upload-large-files-to-aws-s3-using-multipart-upload-and-presigned-urls-4olo

[^4]: https://docs.aws.amazon.com/sdk-for-go/v1/developer-guide/s3-example-presigned-urls.html

[^5]: https://community.transloadit.com/t/presigning-urls-in-batches-for-aws-s3-multipart-upload/15774

[^6]: https://aws.amazon.com/blogs/storage/processing-file-upload-notifications-from-aws-storage-gateway-on-amazon-s3/

[^7]: https://dev.to/mspilari/asynchronous-file-upload-in-java-with-spring-4dnd

[^8]: https://aws.amazon.com/blogs/compute/uploading-large-objects-to-amazon-s3-using-multipart-upload-and-transfer-acceleration/

[^9]: https://cloudviz.io/blog/aws-serverless-architecture-patterns-s3-event-driven-data-processing

[^10]: https://www.reddit.com/r/javahelp/comments/man16e/springboot_concurrent_file_uploads_and_storing_in/

[^11]: https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html

[^12]: https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-pipes-batching-concurrency.html

[^13]: https://stackoverflow.com/questions/36565597/spring-async-file-upload-and-processing

[^14]: https://www.reddit.com/r/aws/comments/1lybi5r/s3_video_upload_presigned_post_vs_put_vs/

[^15]: https://aws.amazon.com/blogs/mt/event-driven-architecture-using-amazon-eventbridge/

[^16]: https://www.baeldung.com/aws-s3-multipart-upload

[^17]: https://www.serverless.com/blog/s3-one-time-signed-url

[^18]: https://docs.aws.amazon.com/transfer/latest/userguide/eventbridge.html

[^19]: https://www.speakeasy.com/api-design/file-uploads

[^20]: https://aws.plainenglish.io/building-an-event-driven-architecture-on-aws-with-amazon-eventbridge-11d49f19554f

[^21]: https://www.youtube.com/watch?v=VGhg6Tfxb60

[^22]: https://www.baeldung.com/cqrs-event-sourcing-java

[^23]: https://blog.bitsrc.io/implementing-a-microservices-application-with-cqrs-command-query-responsibiltiy-segregation-2cecb0b09c66

[^24]: https://johnroest.nl/posts/cqrs-spring-boot-article

[^25]: https://stackoverflow.com/questions/78723286/cqrs-and-domain-driven-design-anemic-data-model-for-query-side

[^26]: https://www.javacodegeeks.com/vertical-slice-architecture.html

[^27]: https://stackoverflow.com/questions/66194613/react-native-uploading-multiple-large-files40-best-practices

[^28]: https://github.com/heynickc/awesome-ddd

[^29]: https://github.com/membrane/spring-boot-vertical-slice-architecture

[^30]: https://www.reddit.com/r/reactnative/comments/sdvz6p/image_upload_best_practices/

[^31]: https://www.youtube.com/watch?v=QXGJCn-1VEo

[^32]: https://www.baeldung.com/java-vertical-slice-architecture

[^33]: https://dev.to/shingaiz/optimizing-file-processing-in-react-with-multipart-uploads-and-downloads-2n8p

[^34]: https://inside.caratlane.com/event-sourcing-and-cqrs-in-spring-boot-aea0d0a359d4

[^35]: https://www.reddit.com/r/dotnet/comments/1eo7uhk/vertical_slice_architecture_the_best_ways_to/

[^36]: https://www.youtube.com/watch?v=uX5E_QFJubU

[^37]: https://www.milanjovanovic.tech/blog/vertical-slice-architecture

[^38]: https://reactnative.dev/docs/images

[^39]: https://www.architecture-weekly.com/p/my-thoughts-on-vertical-slices-cqrs

[^40]: https://uploadcare.com/blog/react-image-optimization-techniques/

