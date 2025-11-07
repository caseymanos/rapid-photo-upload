# Frontend-Backend Integration Guide

This document explains how the React frontend integrates with the Spring Boot backend in the RapidPhotoUpload system.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│  (localhost:3000)                                           │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Upload Page  │  │ Gallery Page │  │  Auth Forms  │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
│         │                  │                  │             │
│         └──────────────────┼──────────────────┘             │
│                            │                                │
│                    ┌───────▼────────┐                      │
│                    │  API Client    │                      │
│                    │  (Axios)       │                      │
│                    └───────┬────────┘                      │
└────────────────────────────┼──────────────────────────────┘
                             │ HTTP/REST
                             │ JWT Auth
┌────────────────────────────▼──────────────────────────────┐
│                  Spring Boot Backend                       │
│  (localhost:8080)                                          │
│                                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │AuthController│  │UploadControl │  │PhotoControl  │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                  │                  │            │
│         └──────────────────┼──────────────────┘            │
│                            │                               │
│                    ┌───────▼────────┐                     │
│                    │  Domain Layer  │                     │
│                    │  (DDD/CQRS)    │                     │
│                    └───────┬────────┘                     │
│                            │                               │
│              ┌─────────────┴─────────────┐                │
│              │                           │                │
│      ┌───────▼────────┐         ┌───────▼────────┐      │
│      │   PostgreSQL    │         │    AWS S3      │      │
│      │   (Metadata)    │         │   (Photos)     │      │
│      └─────────────────┘         └────────────────┘      │
└────────────────────────────────────────────────────────────┘
```

## API Integration Points

### 1. Authentication Flow

#### Frontend Implementation

**Login Process** (`src/features/auth/store/authStore.ts`):

```typescript
// 1. User submits login form
const login = async (data: LoginRequest) => {
  const response = await authApi.login(data);
  const { token, userId, email } = response.data;

  // 2. Store JWT token
  apiClient.setToken(token);
  localStorage.setItem('userId', userId);
  localStorage.setItem('email', email);

  // 3. Update auth state
  setIsAuthenticated(true);
};
```

**API Client** (`src/shared/api/apiClient.ts`):

```typescript
// Axios interceptor adds JWT to all requests
this.client.interceptors.request.use((config) => {
  if (this.token) {
    config.headers.Authorization = `Bearer ${this.token}`;
  }
  return config;
});
```

#### Backend Implementation

**AuthController** (`backend/src/.../feature/auth/AuthController.java`):

```java
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
    // 1. Create user
    User user = userService.register(request);

    // 2. Generate JWT token
    String token = jwtService.generateToken(user);

    // 3. Return token and user info
    return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail()));
}
```

**JWT Validation** (`backend/src/.../infrastructure/security/JwtAuthenticationFilter.java`):

- Intercepts all requests
- Validates JWT token
- Extracts user principal
- Sets SecurityContext

### 2. Photo Upload Flow

This is the most critical integration point, implementing direct-to-S3 upload with presigned URLs.

#### Step 1: Initiate Upload

**Frontend** (`src/features/upload/services/uploadService.ts`):

```typescript
async uploadFile(file: File) {
  // 1. Request upload initiation
  const { data: initResponse } = await uploadApi.initiateUpload({
    filename: file.name,
    fileSizeBytes: file.size,
    mimeType: file.type,
  });

  // Response contains:
  // - photoId: UUID
  // - uploadId: S3 multipart upload ID
  // - presignedUrls: Array of { partNumber, url, expiration }
}
```

**Backend** (`backend/src/.../feature/upload/UploadController.java`):

```java
@PostMapping("/initiate")
public ResponseEntity<InitiateUploadResponse> initiateUpload(
    @RequestBody InitiateUploadRequest request,
    @AuthenticationPrincipal UserPrincipal user
) {
    InitiateUploadCommand command = new InitiateUploadCommand(
        user.getUserId(),
        request.getFilename(),
        request.getFileSizeBytes(),
        request.getMimeType()
    );

    InitiateUploadResponse response = initiateUploadHandler.handle(command);
    return ResponseEntity.ok(response);
}
```

**Handler** (`backend/src/.../application/handler/InitiateUploadHandler.java`):

```java
public InitiateUploadResponse handle(InitiateUploadCommand command) {
    // 1. Create Photo aggregate
    Photo photo = new Photo(UUID.randomUUID(), command.userId(), ...);

    // 2. Initiate S3 multipart upload
    String uploadId = s3StorageService.initiateMultipartUpload(s3Key);

    // 3. Generate presigned URLs for each part (5MB chunks)
    List<PresignedUrl> urls = s3StorageService.generatePresignedUrls(
        s3Key, uploadId, numberOfParts
    );

    // 4. Update domain model and save
    photo.initiateUpload(uploadId);
    photoRepository.save(photo);

    return new InitiateUploadResponse(photo.getId(), uploadId, s3Key, urls);
}
```

#### Step 2: Upload Parts to S3

**Frontend** (`src/features/upload/services/uploadService.ts`):

```typescript
private async uploadParts(file: File, presignedUrls: PresignedUrl[]) {
  const chunks = this.splitFile(file, CHUNK_SIZE); // 5MB chunks

  // Upload all parts in parallel directly to S3
  const uploadPromises = presignedUrls.map(async (presigned, index) => {
    const chunk = chunks[index];

    // Direct PUT to S3 (no backend involved)
    const response = await fetch(presigned.url, {
      method: 'PUT',
      body: chunk,
      headers: { 'Content-Type': file.type },
    });

    // Extract ETag from response
    const etag = response.headers.get('ETag');

    return { partNumber: presigned.partNumber, etag };
  });

  return await Promise.all(uploadPromises);
}
```

**Key Points**:
- Frontend uploads directly to S3 (no backend proxy)
- Reduces backend bandwidth usage
- Enables true parallel uploads
- Backend never handles binary data

#### Step 3: Complete Upload

**Frontend** (`src/features/upload/services/uploadService.ts`):

```typescript
// After all parts uploaded to S3
await uploadApi.completeUpload(photoId, {
  uploadId,
  parts: uploadedParts, // Array of { partNumber, etag }
});
```

**Backend** (`backend/src/.../feature/upload/UploadController.java`):

```java
@PostMapping("/{photoId}/complete")
public ResponseEntity<Void> completeUpload(
    @PathVariable UUID photoId,
    @RequestBody CompleteUploadRequest request
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
```

**Handler** (`backend/src/.../application/handler/CompleteUploadHandler.java`):

```java
public void handle(CompleteUploadCommand command) {
    // 1. Load photo aggregate
    Photo photo = photoRepository.findById(command.photoId());

    // 2. Complete multipart upload with S3
    String etag = s3StorageService.completeMultipartUpload(
        photo.getS3Key(),
        command.uploadId(),
        command.parts()
    );

    // 3. Update domain model
    photo.completeUpload(etag);
    photoRepository.save(photo);

    // 4. Publish domain event
    eventPublisher.publish(new PhotoUploadCompleted(photo.getId()));
}
```

### 3. Gallery Integration

#### Fetching Photos

**Frontend** (`src/features/gallery/hooks/usePhotos.ts`):

```typescript
const fetchPhotos = async () => {
  const response = await photoApi.getPhotos();
  setPhotos(response.data);
};
```

**Backend** (`backend/src/.../feature/photo/PhotoController.java`):

```java
@GetMapping
public ResponseEntity<List<PhotoResponse>> getPhotos(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "50") int size,
    @AuthenticationPrincipal UserPrincipal user
) {
    GetPhotosQuery query = new GetPhotosQuery(user.getUserId(), page, size);
    List<PhotoResponse> photos = getPhotosHandler.handle(query);
    return ResponseEntity.ok(photos);
}
```

#### Photo URLs

**Frontend** (`src/features/gallery/components/PhotoCard.tsx`):

```typescript
const getPhotoUrl = (photo: PhotoResponse) => {
  // Use thumbnail if available
  if (photo.thumbnailUrl) {
    return photo.thumbnailUrl;
  }
  // Otherwise construct S3 URL
  return `https://${photo.s3Bucket}.s3.amazonaws.com/${photo.s3Key}`;
};
```

**Backend** - S3 bucket configured for public read access or CloudFront distribution

#### Updating Metadata

**Frontend** (`src/features/gallery/hooks/usePhotos.ts`):

```typescript
const updatePhotoMetadata = async (photoId: string, tags?: string[], metadata?: any) => {
  await photoApi.updatePhotoMetadata(photoId, { tags, metadata });
  await fetchPhotos(); // Refresh
};
```

**Backend** (`backend/src/.../feature/photo/PhotoController.java`):

```java
@PutMapping("/{photoId}/metadata")
public ResponseEntity<PhotoResponse> updateMetadata(
    @PathVariable UUID photoId,
    @RequestBody UpdatePhotoMetadataRequest request
) {
    UpdatePhotoMetadataCommand command = new UpdatePhotoMetadataCommand(
        photoId,
        request.getTags(),
        request.getMetadata()
    );

    PhotoResponse response = updateMetadataHandler.handle(command);
    return ResponseEntity.ok(response);
}
```

## Concurrency Management

### Frontend Concurrency Control

**Upload Manager** (`src/features/upload/hooks/useUploadManager.ts`):

```typescript
const useUploadManager = (options = { concurrency: 10 }) => {
  // Queue management
  const queueRef = useRef<string[]>([]);
  const [activeUploads, setActiveUploads] = useState(0);

  const processQueue = async () => {
    while (queueRef.current.length > 0 && activeUploads < concurrency) {
      const uploadId = queueRef.current.shift();
      // Start upload (increments activeUploads)
      uploadFile(uploadId);
    }
  };
};
```

**Key Features**:
- Limits to 10 simultaneous uploads
- Queues remaining uploads
- Automatically processes queue as uploads complete
- Prevents browser/network overload

### Backend Concurrency Control

**Async Configuration** (`backend/src/.../infrastructure/config/AsyncConfig.java`):

```java
@Bean
public ThreadPoolTaskExecutor uploadTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);
    executor.setMaxPoolSize(100);
    executor.setQueueCapacity(500);
    return executor;
}
```

**Async Methods**:

```java
@Async
public CompletableFuture<String> completeMultipartUpload(...) {
    // Non-blocking S3 operations
}
```

## Error Handling

### Frontend Error Handling

**API Client** (`src/shared/api/apiClient.ts`):

```typescript
// Response interceptor
this.client.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Unauthorized - clear token and redirect
      this.clearToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

**Upload Service**:

```typescript
try {
  await uploadService.uploadFile(file, options);
  updateUploadStatus(uploadId, 'completed');
} catch (error: any) {
  if (error.name === 'AbortError') {
    updateUploadStatus(uploadId, 'failed', 'Upload cancelled');
  } else {
    updateUploadStatus(uploadId, 'failed', error.message);
  }
}
```

### Backend Error Handling

**Global Exception Handler** (`backend/src/.../shared/exception/GlobalExceptionHandler.java`):

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(ex.getMessage()));
}
```

## Performance Optimizations

### Frontend Optimizations

1. **Chunked Uploads**: 5MB chunks for optimal S3 multipart performance
2. **Parallel Processing**: Upload multiple files simultaneously
3. **Progress Throttling**: Update UI at reasonable intervals
4. **Lazy Loading**: Gallery images load on demand
5. **Optimistic UI**: Immediate feedback before server confirmation

### Backend Optimizations

1. **Direct S3 Upload**: Offloads bandwidth from backend
2. **Async Processing**: Non-blocking I/O for S3 operations
3. **Connection Pooling**: Database connections (min: 10, max: 50)
4. **Thread Pool**: Handles 100 concurrent operations
5. **Presigned URLs**: 2-hour expiration for security

## Security Considerations

### Frontend Security

1. **JWT Storage**: Tokens in localStorage (consider httpOnly cookies for production)
2. **Token Expiration**: Automatic logout on 401 responses
3. **Input Validation**: File type and size validation before upload
4. **HTTPS Required**: All production traffic must use HTTPS

### Backend Security

1. **JWT Validation**: All endpoints protected except auth
2. **User Authorization**: Users can only access their own photos
3. **S3 Presigned URLs**: Time-limited, part-specific access
4. **Input Sanitization**: All user inputs validated
5. **Rate Limiting**: Prevents abuse (configured in API Gateway)

## Testing the Integration

### End-to-End Test Scenario

1. **Register** → `POST /api/v1/auth/register`
2. **Login** → `POST /api/v1/auth/login` (receive JWT)
3. **Initiate Upload** → `POST /api/v1/uploads/initiate` (with JWT)
4. **Upload to S3** → `PUT <presigned-url>` (direct to S3)
5. **Complete Upload** → `POST /api/v1/uploads/{photoId}/complete` (with JWT)
6. **Fetch Photos** → `GET /api/v1/photos` (with JWT)
7. **View Photo** → Access S3 URL from response
8. **Update Metadata** → `PUT /api/v1/photos/{photoId}/metadata` (with JWT)

### Testing 100 Concurrent Uploads

1. Prepare 100 test images
2. Select all files in upload UI
3. Observe:
   - All 100 files queued immediately
   - 10 uploads active at a time
   - Real-time progress updates
   - UI remains responsive
   - All uploads complete successfully

## Troubleshooting

### Common Issues

**CORS Errors**:
- Ensure backend allows `http://localhost:3000` in CORS configuration
- Check `@CrossOrigin` annotation on controllers

**401 Unauthorized**:
- Verify JWT token in localStorage
- Check token expiration
- Ensure `Authorization: Bearer <token>` header is set

**S3 Upload Failures**:
- Verify AWS credentials in backend
- Check S3 bucket permissions
- Ensure presigned URLs haven't expired (2-hour window)

**Connection Refused**:
- Verify backend is running on port 8080
- Check Vite proxy configuration
- Ensure PostgreSQL is running

## Next Steps

1. **Production Deployment**:
   - Configure CloudFront for S3 photo delivery
   - Set up proper CORS policies
   - Use httpOnly cookies for JWT tokens
   - Implement rate limiting

2. **Enhanced Features**:
   - Real-time progress via WebSockets
   - Background thumbnail generation
   - Image compression
   - Advanced search and filtering

3. **Monitoring**:
   - Add error tracking (Sentry)
   - Monitor upload success rates
   - Track performance metrics
   - Set up alerting

## Summary

The frontend and backend integration is designed for:
- **High Performance**: Direct S3 uploads, parallel processing
- **Scalability**: Can handle 100+ concurrent uploads
- **Security**: JWT authentication, presigned URLs
- **Reliability**: Robust error handling, retry logic
- **User Experience**: Real-time progress, responsive UI

The architecture follows best practices with clear separation of concerns, making it maintainable and extensible.
