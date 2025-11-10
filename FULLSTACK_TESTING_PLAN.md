# RapidPhotoUpload Fullstack Testing Plan

## Deployment Status ✅

### Backend (AWS ECS Fargate)
- **Load Balancer URL**: http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com
- **Health Check**: PASSING (status: UP)
- **Container**: Running on ECS Fargate (1 task, ACTIVE)
- **Database**: PostgreSQL RDS connected
- **Migrations**: Flyway v001 applied successfully
- **Docker Image**: linux/amd64, 201MB, pushed to ECR
- **Logs**: Available at CloudWatch `/ecs/rapid-photo-upload-dev`

### Infrastructure
- **S3 Bucket**: rapid-photo-upload-dev-photos-971422717446
- **Database**: rapid-photo-upload-dev-db.crws0amqe1e3.us-east-1.rds.amazonaws.com
- **VPC**: vpc-03cd6462b46350c8e
- **Region**: us-east-1
- **Environment**: dev

## Testing Objectives

### 1. Backend API Testing

#### Authentication Endpoints
- [ ] **POST /api/auth/register** - User registration
  ```bash
  curl -X POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
  ```

- [ ] **POST /api/auth/login** - User login
  ```bash
  curl -X POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","password":"password123"}'
  ```

#### Upload Endpoints
- [ ] **POST /api/upload/initiate** - Initiate upload session
  ```bash
  curl -X POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/upload/initiate \
    -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"fileName":"test.jpg","fileSize":1024000,"contentType":"image/jpeg","totalParts":5}'
  ```

- [ ] **POST /api/upload/parts** - Get presigned URLs for parts
  ```bash
  curl -X POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/upload/parts \
    -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"sessionId":"SESSION_ID","partNumbers":[1,2,3,4,5]}'
  ```

- [ ] **POST /api/upload/complete** - Complete multipart upload
  ```bash
  curl -X POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/upload/complete \
    -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"sessionId":"SESSION_ID","parts":[{"partNumber":1,"eTag":"ETAG1"}]}'
  ```

- [ ] **GET /api/upload/{sessionId}/status** - Get upload status

#### Health & Monitoring
- [x] **GET /actuator/health** - Application health (PASSING)
- [ ] **GET /actuator/info** - Application info
- [ ] **GET /actuator/metrics** - Application metrics

#### WebSocket
- [ ] **WS /ws** - WebSocket connection for real-time progress

### 2. Frontend Web Testing (React + Vite)

#### Configuration
1. Update `frontend-web/.env` with backend URL:
   ```env
   VITE_API_BASE_URL=http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com
   VITE_WS_URL=ws://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/ws
   ```

#### Test Cases
- [ ] Start development server: `cd frontend-web && npm run dev`
- [ ] Test user registration through UI
- [ ] Test user login through UI
- [ ] Test file selection (single file)
- [ ] Test file selection (multiple files - 10-50 files)
- [ ] Test drag-and-drop file upload
- [ ] Test upload progress visualization
- [ ] Test concurrent upload of multiple files
- [ ] Test upload cancellation
- [ ] Test error handling (network errors, server errors)
- [ ] Test upload retry on failure
- [ ] Verify WebSocket real-time progress updates
- [ ] Test upload completion notification
- [ ] Verify uploaded files list

### 3. Mobile App Testing (React Native + Expo)

#### Configuration
1. Update `mobile-app/.env` or `mobile-app/src/config/environment.ts`:
   ```typescript
   export const API_BASE_URL = 'http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com';
   export const WS_URL = 'ws://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/ws';
   ```

#### Test Cases (iOS Simulator/Device)
- [ ] Start Expo: `cd mobile-app && npx expo start`
- [ ] Test authentication on mobile
- [ ] Test photo selection from camera roll
- [ ] Test camera capture and upload
- [ ] Test multi-photo selection (10+ photos)
- [ ] Test upload progress on mobile
- [ ] Test background upload capability
- [ ] Test network interruption handling
- [ ] Test app backgrounding during upload
- [ ] Test upload notification on completion

#### Test Cases (Android Simulator/Device)
- [ ] Same as iOS test cases above

### 4. Integration Testing

#### End-to-End Upload Flow
- [ ] **Web → Backend → S3**: Upload file from web app, verify in S3
- [ ] **Mobile → Backend → S3**: Upload file from mobile app, verify in S3
- [ ] **Concurrent Uploads**: Multiple users uploading simultaneously
- [ ] **Large File Upload**: Upload file >100MB
- [ ] **Many Small Files**: Upload 1000+ small files (1KB each)

#### Database Verification
- [ ] Check upload_sessions table has records
- [ ] Check upload_parts table has part records
- [ ] Verify session status updates (INITIATED → IN_PROGRESS → COMPLETED)
- [ ] Verify user records are created

#### S3 Verification
```bash
# List files in S3 bucket
aws s3 ls s3://rapid-photo-upload-dev-photos-971422717446/ --recursive

# Download and verify file integrity
aws s3 cp s3://rapid-photo-upload-dev-photos-971422717446/uploads/user-id/file-name.jpg ./downloaded.jpg
```

#### EventBridge Verification
- [ ] Check events are published to EventBridge
- [ ] Verify event structure matches schema

### 5. Performance Testing

#### Load Testing
```bash
# Install Apache Bench
brew install apache-bench

# Test with 10 concurrent users
ab -n 100 -c 10 http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health

# Test upload endpoint
ab -n 50 -c 5 -p upload-payload.json -T application/json \
  -H "Authorization: Bearer YOUR_TOKEN" \
  http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/upload/initiate
```

#### Metrics to Monitor
- [ ] Average response time (should be <500ms for API calls)
- [ ] Upload throughput (MB/s)
- [ ] Concurrent upload capacity
- [ ] ECS task CPU usage (should stay <80%)
- [ ] ECS task memory usage (should stay <80%)
- [ ] RDS connection pool usage
- [ ] S3 request rate and errors

#### CloudWatch Monitoring
```bash
# Check ECS service metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=rapid-photo-upload-dev \
  --start-time 2025-11-09T00:00:00Z \
  --end-time 2025-11-09T23:59:59Z \
  --period 300 \
  --statistics Average

# Check application logs
aws logs tail /ecs/rapid-photo-upload-dev --follow --since 5m
```

### 6. Security Testing

#### CORS Configuration
- [ ] Verify frontend origin is allowed
- [ ] Test cross-origin requests from web app
- [ ] Test cross-origin WebSocket connections
- [ ] Verify unauthorized origins are blocked

#### Authentication & Authorization
- [ ] Test JWT token generation
- [ ] Test JWT token validation
- [ ] Test expired token rejection
- [ ] Test invalid token rejection
- [ ] Test unauthorized endpoint access
- [ ] Verify password hashing (bcrypt)

#### S3 Security
- [ ] Test presigned URL expiration (should fail after timeout)
- [ ] Verify presigned URLs are specific to user/session
- [ ] Test unauthorized S3 access attempts
- [ ] Verify bucket policies restrict public access

#### Secrets Management
```bash
# Verify JWT secret is in Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id rapid-photo-upload-dev-jwt-secret

# Verify DB credentials are in Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id rapid-photo-upload-dev-db-credentials
```

### 7. Infrastructure Validation

#### CloudFormation Stacks
```bash
# Check all stacks are deployed
aws cloudformation describe-stacks \
  --query 'Stacks[?contains(StackName, `rapid-photo-upload-dev`)].{Name:StackName,Status:StackStatus}' \
  --output table
```

Expected stacks:
- [x] rapid-photo-upload-dev-network (CREATE_COMPLETE)
- [x] rapid-photo-upload-dev-storage (CREATE_COMPLETE)
- [x] rapid-photo-upload-dev-database (CREATE_COMPLETE)
- [x] rapid-photo-upload-dev-event (CREATE_COMPLETE)
- [x] rapid-photo-upload-dev-compute (CREATE_COMPLETE)

#### ECS Service Health
```bash
# Check service status
aws ecs describe-services \
  --cluster rapid-photo-upload-dev \
  --services rapid-photo-upload-dev \
  --query 'services[0].{Status:status,RunningCount:runningCount,DesiredCount:desiredCount,HealthCheck:healthCheckGracePeriodSeconds}'
```

#### ALB Target Health
```bash
# Check target group health
aws elbv2 describe-target-health \
  --target-group-arn $(aws elbv2 describe-target-groups \
    --query 'TargetGroups[?contains(TargetGroupName, `rapid-photo-upload-dev`)].TargetGroupArn' \
    --output text)
```

#### RDS Instance
```bash
# Check RDS status
aws rds describe-db-instances \
  --db-instance-identifier rapid-photo-upload-dev-db \
  --query 'DBInstances[0].{Status:DBInstanceStatus,Endpoint:Endpoint.Address,Port:Endpoint.Port}'
```

### 8. Error Scenarios & Edge Cases

#### Network Failures
- [ ] Test upload with network interruption (mid-upload)
- [ ] Test retry mechanism on transient failures
- [ ] Test timeout handling

#### Server Errors
- [ ] Test with invalid JWT token
- [ ] Test with expired session
- [ ] Test with corrupted file data
- [ ] Test with file size exceeding limits

#### Capacity Testing
- [ ] Test maximum concurrent users
- [ ] Test database connection pool exhaustion
- [ ] Test S3 rate limit handling
- [ ] Test ECS auto-scaling (if configured)

### 9. Monitoring & Logging

#### CloudWatch Logs
```bash
# Application logs
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Filter for errors
aws logs tail /ecs/rapid-photo-upload-dev --follow --filter-pattern "ERROR"

# Filter for specific session
aws logs tail /ecs/rapid-photo-upload-dev --follow --filter-pattern "sessionId=SESSION_ID"
```

#### CloudWatch Alarms (if configured)
- [ ] Test high CPU alarm
- [ ] Test high memory alarm
- [ ] Test failed health check alarm
- [ ] Test database connection failure alarm

### 10. Cleanup & Rollback Testing

#### Graceful Shutdown
- [ ] Test application graceful shutdown
- [ ] Verify in-progress uploads complete or are marked for retry
- [ ] Test database connection cleanup

#### Stack Deletion (for cleanup)
```bash
# Delete compute stack
aws cloudformation delete-stack --stack-name rapid-photo-upload-dev-compute

# Delete all stacks in reverse order
aws cloudformation delete-stack --stack-name rapid-photo-upload-dev-database
aws cloudformation delete-stack --stack-name rapid-photo-upload-dev-event
aws cloudformation delete-stack --stack-name rapid-photo-upload-dev-storage
aws cloudformation delete-stack --stack-name rapid-photo-upload-dev-network
```

## Success Criteria

### Functional Requirements
- ✅ All API endpoints respond with correct status codes
- ✅ JWT authentication works correctly
- ✅ Multipart upload flow completes successfully
- ✅ Files appear in S3 bucket after upload
- ✅ Database records are created correctly
- ✅ WebSocket real-time updates work
- ✅ Frontend can upload files successfully
- ✅ Mobile app can upload files successfully

### Performance Requirements
- Response time < 500ms for API calls
- Upload throughput > 10 MB/s per user
- Support 50+ concurrent users
- ECS CPU usage < 80% under normal load
- Database connection pool healthy

### Security Requirements
- CORS properly configured
- JWT tokens validated on all protected endpoints
- S3 presigned URLs expire correctly
- Database credentials stored in Secrets Manager
- No sensitive data in logs

### Reliability Requirements
- Health checks passing
- Auto-restart on container failure
- Graceful handling of network interruptions
- Proper error messages returned to clients

## Known Issues & Workarounds

### Issue 1: JWT Secret Permissions ✅ FIXED
- **Problem**: ECS tasks couldn't read JWT secret from Secrets Manager
- **Solution**: Changed to `Secret.fromSecretCompleteArn()` and added explicit `grantRead()`
- **Status**: RESOLVED

### Issue 2: Docker Platform Mismatch ✅ FIXED
- **Problem**: Image built for ARM64 (Apple Silicon) but ECS needs linux/amd64
- **Solution**: Rebuilt with `docker build --platform linux/amd64`
- **Status**: RESOLVED

## Next Steps

1. **Configure Frontend**: Update .env with ALB URL
2. **Configure Mobile**: Update config with ALB URL
3. **Run API Tests**: Use curl/Postman to test all endpoints
4. **Run Frontend Tests**: Test web app upload flows
5. **Run Mobile Tests**: Test mobile app upload flows
6. **Performance Tests**: Run load tests
7. **Security Audit**: Verify all security controls
8. **Documentation**: Document any issues found and resolutions

## Testing Tools

- **API Testing**: curl, Postman, HTTPie
- **Load Testing**: Apache Bench, k6, JMeter
- **Frontend Testing**: Browser DevTools, React DevTools
- **Mobile Testing**: Expo Go, iOS Simulator, Android Emulator
- **AWS CLI**: For infrastructure validation
- **CloudWatch**: For monitoring and logging

## Contact & Support

- Backend URL: http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com
- CloudWatch Logs: /ecs/rapid-photo-upload-dev
- S3 Bucket: s3://rapid-photo-upload-dev-photos-971422717446
- Region: us-east-1
