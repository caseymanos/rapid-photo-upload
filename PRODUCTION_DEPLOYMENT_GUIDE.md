# Production Deployment Guide - RapidPhotoUpload

**Status**: Ready for Deployment  
**Target Environment**: AWS (S3 + CloudFront + ECS Fargate)  
**Estimated Deployment Time**: 20-30 minutes

---

## 🎯 Pre-Deployment Checklist

- ✅ Backend deployed on AWS ECS Fargate (rapid-photo-upload-dev)
- ✅ RDS PostgreSQL database running (rapid-photo-upload-dev-db)
- ✅ S3 bucket for photos (rapid-photo-upload-dev-photos-971422717446)
- ✅ Frontend CDK stack created (infrastructure/lib/frontend-stack.ts)
- ✅ Backend CORS updated for CloudFront
- ✅ Mobile app EAS configuration ready

---

## 📋 Deployment Steps

### Step 1: Deploy Frontend to CloudFront (15 min)

#### 1.1 Build Frontend Application
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/frontend-web

# Install dependencies (if not done)
npm install

# Build production bundle
npm run build

# Verify build output
ls -lh dist/
# Should see: index.html, assets/ folder with .js and .css files
```

#### 1.2 Deploy CloudFront + S3 Stack
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/infrastructure

# Ensure AWS credentials are configured
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1

# Deploy frontend stack
cdk deploy rapid-photo-upload-dev-frontend --require-approval never

# Expected output:
# ✅ rapid-photo-upload-dev-frontend
# 
# Outputs:
# rapid-photo-upload-dev-frontend.WebsiteUrl = https://d1234567890.cloudfront.net
# rapid-photo-upload-dev-frontend.DistributionId = E1234567890ABC
# rapid-photo-upload-dev-frontend.BucketName = rapid-photo-upload-dev-web
```

**⚠️ IMPORTANT**: Copy the CloudFront URL from the output. You'll need it for the next steps.

#### 1.3 Manual Deployment (If CDK S3 Deployment Fails)
```bash
# If CDK deployment doesn't upload files, do it manually:
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/frontend-web

# Upload to S3
aws s3 sync dist/ s3://rapid-photo-upload-dev-web --delete

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id E1234567890ABC \
  --paths "/*"
```

---

### Step 2: Update Backend CORS (Already Done)

The backend CORS has been updated to accept CloudFront origins:
```java
// backend/.../SecurityConfig.java (line 66)
"https://*.cloudfront.net"  // Wildcard for any CloudFront distribution
```

**Optional**: For stricter security, replace the wildcard with your specific CloudFront URL:
```java
"https://d1234567890.cloudfront.net"  // Your specific CloudFront distribution
```

If you make this change:
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/backend

# Rebuild Docker image
docker build --platform linux/amd64 -t rapid-photo-upload:latest .

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 971422717446.dkr.ecr.us-east-1.amazonaws.com
docker tag rapid-photo-upload:latest 971422717446.dkr.ecr.us-east-1.amazonaws.com/rapid-photo-upload-dev:latest
docker push 971422717446.dkr.ecr.us-east-1.amazonaws.com/rapid-photo-upload-dev:latest

# Force ECS service update (rolling deployment)
aws ecs update-service \
  --cluster rapid-photo-upload-dev \
  --service rapid-photo-upload-dev \
  --force-new-deployment

# Monitor deployment (takes ~3-5 minutes)
watch -n 5 'aws ecs describe-services --cluster rapid-photo-upload-dev --services rapid-photo-upload-dev --query "services[0].{Status:status,Running:runningCount,Desired:desiredCount,Health:healthCheckGracePeriodSeconds}"'
```

---

### Step 3: Deploy Mobile App to Expo (10 min)

#### 3.1 Setup EAS CLI
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app

# Install EAS CLI globally (if not installed)
npm install -g eas-cli

# Login to Expo
eas login
# Enter your Expo credentials
```

#### 3.2 Initialize EAS Project
```bash
# Initialize EAS project (creates project ID)
eas init

# Update app.config.js with the project ID from output
# Edit: extra.eas.projectId = "your-project-id-here"
```

#### 3.3 Build Development Version
```bash
# Build for iOS simulator + Android emulator
eas build --platform all --profile development

# Expected output:
# ✅ iOS build: https://expo.dev/accounts/your-account/projects/rapid-photo-upload/builds/abc123
# ✅ Android build: https://expo.dev/accounts/your-account/projects/rapid-photo-upload/builds/def456
```

#### 3.4 Test Mobile App
```bash
# Start Expo dev server
npx expo start

# On iOS simulator:
# Press 'i' to open in simulator

# On Android emulator:
# Press 'a' to open in emulator

# On physical device:
# Scan QR code with Expo Go app
```

---

## ✅ Post-Deployment Verification

### Frontend Web App

#### 1. Access CloudFront URL
```bash
# Open in browser:
https://d1234567890.cloudfront.net

# Expected: React app loads, shows login/register page
```

#### 2. Test User Registration
1. Navigate to Register page
2. Enter email: `test@example.com`
3. Enter password: `TestPassword123!`
4. Click Register
5. **Expected**: JWT token received, redirected to upload page

#### 3. Test File Upload
1. Login with test credentials
2. Navigate to Upload page
3. Select a photo (or drag-and-drop)
4. **Expected**: Upload progress shows, completes successfully

#### 4. Check Browser Console
```javascript
// Open DevTools (F12)
// Network tab should show:
// ✅ POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1/auth/register
// ✅ POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1/auth/login
// ✅ POST http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1/uploads/initiate

// Console should show NO CORS errors
```

---

### Mobile App

#### 1. Test Authentication
1. Open app on simulator/device
2. Register new user
3. Login
4. **Expected**: JWT token stored, navigates to home screen

#### 2. Test Photo Upload
1. Select photo from library (or take new photo)
2. Initiate upload
3. **Expected**: Progress bar shows, upload completes

#### 3. Check Expo Logs
```bash
# In terminal running 'npx expo start':
# Should see:
# ✅ API calls to backend
# ✅ No network errors
# ✅ Upload progress updates
```

---

### Backend Monitoring

#### 1. Check ECS Service Health
```bash
aws ecs describe-services \
  --cluster rapid-photo-upload-dev \
  --services rapid-photo-upload-dev \
  --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount}'

# Expected output:
# {
#   "Status": "ACTIVE",
#   "Running": 1,
#   "Desired": 1
# }
```

#### 2. Check Application Logs
```bash
# View live logs
aws logs tail /ecs/rapid-photo-upload-dev --follow --since 5m

# Filter for errors
aws logs tail /ecs/rapid-photo-upload-dev --follow --filter-pattern "ERROR"

# Check for CORS logs
aws logs tail /ecs/rapid-photo-upload-dev --follow --filter-pattern "CORS"
```

#### 3. Check S3 Uploads
```bash
# List uploaded files
aws s3 ls s3://rapid-photo-upload-dev-photos-971422717446/uploads/ --recursive --human-readable

# Expected: Files uploaded from frontend/mobile should appear
```

---

## 🔧 Troubleshooting

### Issue 1: CloudFront Returns 403 Forbidden

**Symptoms**: Accessing CloudFront URL shows XML error  
**Cause**: S3 bucket not properly configured or files not uploaded

**Solution**:
```bash
# Verify files in S3
aws s3 ls s3://rapid-photo-upload-dev-web/ --recursive

# If empty, manually upload:
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/frontend-web
aws s3 sync dist/ s3://rapid-photo-upload-dev-web --delete

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id [YOUR-DISTRIBUTION-ID] \
  --paths "/*"
```

---

### Issue 2: CORS Errors in Browser Console

**Symptoms**: Console shows "Access to XMLHttpRequest... has been blocked by CORS policy"  
**Cause**: Backend CORS not allowing CloudFront origin

**Solution**:
```bash
# Verify current CORS configuration
curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health \
  -H "Origin: https://d1234567890.cloudfront.net" \
  -H "Access-Control-Request-Method: POST" \
  -X OPTIONS -v

# Should see: Access-Control-Allow-Origin: https://d1234567890.cloudfront.net

# If not, update SecurityConfig.java and redeploy backend
```

---

### Issue 3: Mobile App Can't Connect to Backend

**Symptoms**: Network request failed errors in Expo  
**Cause**: Incorrect API URL in app.config.js

**Solution**:
```bash
# Verify app.config.js has correct URL
cat /Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app/app.config.js | grep apiUrl

# Should show: http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1

# If incorrect, update and restart Expo:
npx expo start --clear
```

---

### Issue 4: CloudFront Distribution Takes Too Long to Deploy

**Symptoms**: CDK deployment stuck on "Creating CloudFront distribution"  
**Cause**: CloudFront global distribution takes 15-20 minutes on first deployment

**Solution**: Wait patiently. This is normal for CloudFront initial deployment.

---

## 📊 Performance Testing

### Test 100 Concurrent Uploads (Per PRD Requirement)

#### 1. Prepare Test Files
```bash
# Create 100 test images (2MB each)
mkdir -p /tmp/test-photos
for i in {1..100}; do
  dd if=/dev/urandom of=/tmp/test-photos/photo-$i.jpg bs=2M count=1
done
```

#### 2. Run Upload Test (Frontend)
```javascript
// Open browser console on CloudFront URL
// Paste and run:

async function testConcurrentUploads() {
  const files = [];
  
  // Create 100 file objects
  for (let i = 0; i < 100; i++) {
    const blob = new Blob([new Uint8Array(2 * 1024 * 1024)], { type: 'image/jpeg' });
    files.push(new File([blob], `photo-${i}.jpg`, { type: 'image/jpeg' }));
  }
  
  console.log(`Starting upload of ${files.length} files...`);
  const startTime = Date.now();
  
  // Upload all files concurrently
  const uploadPromises = files.map(file => {
    // Your upload function here
    return uploadFile(file);
  });
  
  await Promise.all(uploadPromises);
  
  const endTime = Date.now();
  const duration = (endTime - startTime) / 1000;
  
  console.log(`✅ Uploaded ${files.length} files in ${duration} seconds`);
  console.log(`Target: <90 seconds. Result: ${duration < 90 ? 'PASS ✅' : 'FAIL ❌'}`);
}

testConcurrentUploads();
```

#### 3. Monitor Backend Performance
```bash
# Watch ECS CPU/Memory during test
watch -n 2 'aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=rapid-photo-upload-dev \
  --start-time $(date -u -d "5 minutes ago" +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Average \
  --query "Datapoints[0].Average"'
```

---

## 💰 Cost Monitoring

### Monthly Cost Estimate
```bash
# View current month costs
aws ce get-cost-and-usage \
  --time-period Start=$(date -u -d "1 month ago" +%Y-%m-%d),End=$(date -u +%Y-%m-%d) \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --group-by Type=SERVICE \
  --filter file://<(echo '{"Tags":{"Key":"Project","Values":["rapid-photo-upload"]}}')

# Expected monthly cost: $72-131 USD
# - ECS Fargate: $30-60
# - RDS PostgreSQL: $15
# - S3: $1-5
# - CloudFront: $5-15
# - ALB: $16
# - Data Transfer: $5-20
```

---

## 🎯 Success Criteria

- ✅ Frontend accessible via CloudFront HTTPS URL
- ✅ Backend API responds to frontend requests (no CORS errors)
- ✅ Mobile app connects to backend successfully
- ✅ User registration works (JWT tokens issued)
- ✅ User login works (JWT tokens validated)
- ✅ File upload initiation works (presigned URLs returned)
- ✅ 100 concurrent uploads complete in <90 seconds
- ✅ UI remains responsive during uploads
- ✅ WebSocket real-time updates work
- ✅ All CloudWatch metrics healthy (CPU <70%, Memory <80%)

---

## 📚 Useful Commands Reference

### Frontend
```bash
# Build production
cd frontend-web && npm run build

# Deploy to S3
aws s3 sync dist/ s3://rapid-photo-upload-dev-web --delete

# Invalidate CloudFront
aws cloudfront create-invalidation --distribution-id [ID] --paths "/*"
```

### Backend
```bash
# Rebuild and push Docker image
cd backend
docker build --platform linux/amd64 -t rapid-photo-upload:latest .
docker tag rapid-photo-upload:latest 971422717446.dkr.ecr.us-east-1.amazonaws.com/rapid-photo-upload-dev:latest
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 971422717446.dkr.ecr.us-east-1.amazonaws.com
docker push 971422717446.dkr.ecr.us-east-1.amazonaws.com/rapid-photo-upload-dev:latest

# Force ECS redeploy
aws ecs update-service --cluster rapid-photo-upload-dev --service rapid-photo-upload-dev --force-new-deployment
```

### Mobile
```bash
# Build development version
cd mobile-app
eas build --platform all --profile development

# Build preview version
eas build --platform all --profile preview

# Start Expo dev server
npx expo start
```

### Monitoring
```bash
# View ECS logs
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Check ECS service status
aws ecs describe-services --cluster rapid-photo-upload-dev --services rapid-photo-upload-dev

# List S3 uploads
aws s3 ls s3://rapid-photo-upload-dev-photos-971422717446/uploads/ --recursive
```

---

## 🔄 Rollback Procedure

If deployment fails or issues arise:

### Rollback Frontend
```bash
# Delete CloudFront stack
cd infrastructure
cdk destroy rapid-photo-upload-dev-frontend

# This will:
# - Delete CloudFront distribution
# - Delete S3 bucket (if autoDeleteObjects: true)
# - Remove all frontend infrastructure
```

### Rollback Backend
```bash
# Revert to previous ECS task definition
aws ecs update-service \
  --cluster rapid-photo-upload-dev \
  --service rapid-photo-upload-dev \
  --task-definition rapid-photo-upload-dev:[PREVIOUS-REVISION]
```

### Rollback Mobile
```bash
# Previous Expo builds remain accessible
# Just point users to previous build URL
```

---

**Last Updated**: November 9, 2025  
**Deployment Status**: Ready for Production  
**Support**: Check CloudWatch logs and ECS service status for issues

