# Agent Context: RapidPhotoUpload Project

**For**: Next Agent Starting Fresh
**Date**: 2025-11-09
**Project**: RapidPhotoUpload - High-performance photo upload system

---

## 🎯 What You Need to Know

This is a **95% complete** full-stack application with backend deployed to AWS, frontend and mobile apps built and ready to deploy.

**Your role**: Help deploy frontend to CloudFront, test mobile app, and handle any final integration issues.

---

## 📁 Project Structure

```
rapidPhotoUpload/
├── backend/                  # Spring Boot API (DEPLOYED TO AWS)
├── frontend-web/             # React + Vite web app (READY TO DEPLOY)
├── mobile-app/               # React Native + Expo (READY TO DEPLOY)
├── infrastructure/           # AWS CDK stacks (DEPLOYED)
├── CURRENT_STATUS.md         # ← START HERE - Master reference
├── DEPLOYMENT_STATUS.md      # Detailed deployment info
├── INTEGRATION_VERIFICATION.md  # Integration validation
└── .repoprompt/              # This directory
    └── AGENT_CONTEXT.md      # You are here
```

---

## 🚀 Quick Start Guide for New Agent

### Step 1: Read the Docs (5 minutes)

**Must-read** (in order):
1. **CURRENT_STATUS.md** - Complete system overview
2. **DEPLOYMENT_STATUS.md** - What's deployed, what's not
3. **INTEGRATION_VERIFICATION.md** - Frontend/mobile integration details

**Reference docs**:
- `BACKEND_TESTING_RESULTS.md` - API specification
- `FULLSTACK_TESTING_PLAN.md` - Testing procedures
- `infrastructure/README.md` - Infrastructure details

### Step 2: Verify Current State (2 minutes)

```bash
# Check backend is running
curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health

# Check ECS service
aws ecs describe-services \
  --cluster rapid-photo-upload-dev \
  --services rapid-photo-upload-dev \
  --query 'services[0].{Status:status, RunningCount:runningCount}'

# Check if frontend is deployed
aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-frontend 2>&1 | grep -q "does not exist" && echo "NOT DEPLOYED" || echo "DEPLOYED"
```

### Step 3: Test Locally (10 minutes)

**Frontend**:
```bash
cd frontend-web
npm install
npm run dev
# Open http://localhost:3004
# Test: Register → Login → Upload photos
```

**Mobile**:
```bash
cd mobile-app
npm install
npx expo start
# Scan QR code with Expo Go
# Test: Register → Login → Upload photos
```

### Step 4: Deploy to Production (User will handle this)

The user will test locally first, then deploy. Your job is to assist with any issues.

---

## 🔧 System Architecture

### Backend (AWS ECS Fargate)
- **URL**: `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com`
- **API Prefix**: `/api/v1`
- **Stack**: Spring Boot 3.2, Java 21, PostgreSQL 15
- **Features**: JWT auth, multipart S3 upload, EventBridge events

### Frontend (React + Vite)
- **Location**: `frontend-web/`
- **Config**: `.env` has backend URL
- **Deploy To**: CloudFront + S3 (stack: `infrastructure/lib/frontend-stack.ts`)
- **Command**: `cdk deploy rapid-photo-upload-dev-frontend`

### Mobile (React Native + Expo)
- **Location**: `mobile-app/`
- **Config**: `app.config.js` + `.env` have backend URL
- **Deploy To**: EAS (Expo Application Services)
- **Build**: `eas build --profile production --platform all`

### Infrastructure (AWS CDK)
**Deployed Stacks**:
- ✅ NetworkStack - VPC, security groups
- ✅ StorageStack - S3 bucket
- ✅ EventStack - EventBridge
- ✅ DatabaseStack - RDS PostgreSQL
- ✅ ComputeStack - ECS + ALB
- ✅ MonitoringStack - CloudWatch

**Ready to Deploy**:
- ⏳ FrontendStack - CloudFront + S3

---

## 📊 API Contract (Backend)

**Authentication** (no username, email-based):
```typescript
// Register
POST /api/v1/auth/register
Body: { email: string, password: string }
Response: { token: string, userId: string, email: string, expiresIn: number }

// Login
POST /api/v1/auth/login
Body: { email: string, password: string }
Response: { token: string, userId: string, email: string, expiresIn: number }
```

**Upload Flow**:
```typescript
// 1. Initiate upload (get presigned URLs)
POST /api/v1/uploads/initiate
Headers: { Authorization: "Bearer {token}" }
Body: {
  originalFilename: string,   // NOT "fileName"
  fileSizeBytes: number,       // NOT "fileSize"
  mimeType: string            // NOT "contentType"
}
Response: {
  photoId: string,
  s3Key: string,
  multipartUploadId: string,
  presignedUrls: [{ partNumber: number, url: string }],
  expiresAt: string
}

// 2. Upload to S3 (use presigned URL)
PUT {presignedUrl}
Body: [binary file data]
Response: HTTP 200, ETag header

// 3. Complete upload
POST /api/v1/uploads/{photoId}/complete
Headers: { Authorization: "Bearer {token}" }
Body: {
  parts: [{
    partNumber: number,
    etag: string,
    sizeBytes: number
  }]
}
Response: HTTP 200/204
```

**IMPORTANT**: Frontend and mobile apps already use correct field names. No changes needed.

---

## ⚠️ Known Issues

### Issue #1: Upload Completion Bug (FIXED, deploying)

**Status**: 🔄 FIX DEPLOYED, ROLLING OUT

**Was**: Upload completion failed with Hibernate session error
**Fix**: Removed duplicate `photoRepository.save()` calls in `CompleteUploadHandler.java`
**ETA**: Deployment should be complete by the time you read this

**To verify**:
```bash
/tmp/test-complete-flow.sh
# Should complete successfully without errors
```

---

## 🎯 User's Next Steps (What They'll Ask You to Help With)

### 1. Local Testing
**User will**:
- Start frontend: `cd frontend-web && npm run dev`
- Start mobile: `cd mobile-app && npx expo start`
- Test upload flows

**You should**:
- Watch for console errors
- Help debug any API integration issues
- Verify presigned URLs work
- Check CORS configuration if issues arise

### 2. Frontend Deployment to CloudFront
**User will**:
```bash
cd infrastructure
cdk deploy rapid-photo-upload-dev-frontend
```

**You should**:
- Monitor deployment progress
- Capture CloudFront URL from outputs
- Update `.env.production` if needed
- Test the live CloudFront URL
- Help with DNS configuration if custom domain is needed

### 3. Mobile App Build
**User will**:
```bash
cd mobile-app
eas build --profile production --platform all
```

**You should**:
- Help with EAS setup (`eas init` if needed)
- Update project ID in `app.json` if needed
- Monitor build progress
- Help with code signing if issues arise
- Test builds on TestFlight/internal testing

---

## 🔍 Troubleshooting Guide

### Frontend Won't Start
```bash
cd frontend-web
rm -rf node_modules package-lock.json
npm install
npm run dev
```

### Mobile App Won't Start
```bash
cd mobile-app
rm -rf node_modules package-lock.json
npx expo install --fix
npx expo start --clear
```

### API Returns 403
**Cause**: CORS issue
**Check**: `backend/src/main/java/com/rapidphotoupload/infrastructure/config/SecurityConfig.java`
**Allowed Origins**:
- http://localhost:3000
- http://localhost:3004
- http://localhost:19006
- https://*.cloudfront.net

**Fix**: If deploying to custom domain, add it to CORS allowed origins

### Upload Fails
**Check**:
1. Is backend healthy? `curl {backend-url}/actuator/health`
2. Is JWT token valid? Check expiration (24 hours)
3. Are presigned URLs expired? (2 hour expiration)
4. Check CloudWatch logs: `aws logs tail /ecs/rapid-photo-upload-dev --follow`

### ECS Deployment Stuck
```bash
# Check deployment status
aws ecs describe-services \
  --cluster rapid-photo-upload-dev \
  --services rapid-photo-upload-dev

# Check task logs
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Force new deployment if stuck
aws ecs update-service \
  --cluster rapid-photo-upload-dev \
  --service rapid-photo-upload-dev \
  --force-new-deployment
```

---

## 📝 Important Files & Locations

### Configuration Files
- `backend/src/main/resources/application.yml` - Backend config
- `frontend-web/.env` - Frontend dev config
- `frontend-web/.env.production` - Frontend prod config
- `mobile-app/app.config.js` - Mobile app config
- `mobile-app/.env` - Mobile environment variables
- `infrastructure/lib/config.ts` - CDK environment config

### Key Source Files
**Backend**:
- `backend/src/main/java/com/rapidphotoupload/application/handler/CompleteUploadHandler.java` - Upload completion logic (recently fixed)
- `backend/src/main/java/com/rapidphotoupload/infrastructure/config/SecurityConfig.java` - CORS & auth config

**Frontend**:
- `frontend-web/src/shared/api/apiClient.ts` - API client
- `frontend-web/src/features/upload/services/uploadService.ts` - Upload logic
- `frontend-web/src/features/auth/services/authService.ts` - Auth logic

**Mobile**:
- `mobile-app/src/features/upload/services/uploadService.ts` - Upload logic
- `mobile-app/src/features/auth/services/authService.ts` - Auth logic

### Infrastructure
- `infrastructure/lib/frontend-stack.ts` - CloudFront + S3 stack (ready to deploy)
- `infrastructure/lib/compute-stack.ts` - ECS + ALB (deployed)
- `infrastructure/bin/app.ts` - Main CDK app

---

## 🎓 Key Learnings from Development

### What Went Well
✅ Backend deployed cleanly to AWS
✅ Frontend and mobile apps built with correct API contract
✅ Infrastructure fully automated with CDK
✅ Comprehensive monitoring and logging
✅ Type safety across frontend/backend

### What to Watch For
⚠️ Field naming differences (backend uses `originalFilename`, not `fileName`)
⚠️ Backend endpoints use `/api/v1` prefix
⚠️ Auth uses email (no username field)
⚠️ Upload completion requires ETag from S3 response
⚠️ CORS must include CloudFront URL after deployment

### Recent Bug Fixes
🐛 **Hibernate session bug** (fixed Nov 9): Duplicate `save()` calls caused upload completion to fail
🔧 **Environment variables** (fixed Nov 8): Mismatched env var names between CDK and Spring Boot
✅ **CORS configuration** (working): Wildcard for CloudFront + specific localhost ports

---

## 💡 Pro Tips for Success

1. **Always check CURRENT_STATUS.md first** - it's the single source of truth

2. **Test backend API directly** before blaming frontend:
   ```bash
   curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health
   ```

3. **Use CloudWatch logs** for backend debugging:
   ```bash
   aws logs tail /ecs/rapid-photo-upload-dev --follow --filter-pattern "ERROR"
   ```

4. **Frontend/mobile types are already correct** - no changes needed per INTEGRATION_VERIFICATION.md

5. **CDK deployments are idempotent** - safe to re-run if something fails

6. **ECS deployments use rolling updates** - old tasks stay running until new ones are healthy

7. **Presigned URLs expire after 2 hours** - if testing is delayed, re-initiate upload

8. **JWT tokens expire after 24 hours** - users must re-login

---

## 📞 Quick Reference Commands

### Backend Management
```bash
# View logs
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Restart service
aws ecs update-service --cluster rapid-photo-upload-dev --service rapid-photo-upload-dev --force-new-deployment

# Check health
curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health
```

### Frontend Deployment
```bash
cd infrastructure
cdk deploy rapid-photo-upload-dev-frontend
```

### Mobile Build
```bash
cd mobile-app
eas build --profile production --platform all
```

### Infrastructure
```bash
# View all stacks
cdk list

# View stack diff
cdk diff rapid-photo-upload-dev-frontend

# Destroy stack (careful!)
cdk destroy rapid-photo-upload-dev-frontend
```

---

## 🎯 Success Checklist

Use this to verify everything is working:

- [ ] Backend `/actuator/health` returns `{"status":"UP"}`
- [ ] Can register new user via API
- [ ] Can login and receive JWT token
- [ ] Can initiate upload and receive presigned URLs
- [ ] Can upload file to S3 using presigned URL
- [ ] Can complete upload and verify in database
- [ ] Frontend starts locally (`npm run dev`)
- [ ] Mobile app starts locally (`npx expo start`)
- [ ] Frontend can complete full upload flow
- [ ] Mobile can complete full upload flow
- [ ] CloudFront deployment succeeds (if deploying)
- [ ] Mobile build succeeds (if building)

---

## 📚 Additional Resources

**AWS Console Links** (us-east-1):
- ECS Service: https://console.aws.amazon.com/ecs/home?region=us-east-1#/clusters/rapid-photo-upload-dev/services/rapid-photo-upload-dev
- CloudWatch Dashboard: https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=rapid-photo-upload-dev
- RDS Database: https://console.aws.amazon.com/rds/home?region=us-east-1#database:id=rapid-photo-upload-dev-db
- S3 Bucket: https://s3.console.aws.amazon.com/s3/buckets/rapid-photo-upload-dev-photos-971422717446
- CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Fecs$252Frapid-photo-upload-dev

**External Docs**:
- AWS CDK: https://docs.aws.amazon.com/cdk/
- Expo EAS: https://docs.expo.dev/build/introduction/
- React Query: https://tanstack.com/query/latest/docs/framework/react/overview
- Vite: https://vitejs.dev/guide/

---

## ⚡ TL;DR - Start Here

1. **Read**: `CURRENT_STATUS.md` (10 min)
2. **Verify**: Backend is healthy (`curl` health endpoint)
3. **Test**: Start frontend locally (`npm run dev`)
4. **Test**: Start mobile locally (`npx expo start`)
5. **Help**: User deploy to CloudFront/EAS
6. **Monitor**: Watch logs, fix issues as they arise

**Backend URL**: `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com`
**API Prefix**: `/api/v1`
**Everything else**: See `CURRENT_STATUS.md`

---

**Status**: Ready for final testing and deployment
**Blocker**: None (upload completion bug fixed and deploying)
**Next**: User will test locally, then deploy frontend/mobile

Good luck! 🚀
