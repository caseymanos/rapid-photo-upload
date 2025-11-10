# RapidPhotoUpload - Quick Start Guide 🚀

**Status**: ✅ Backend deployed, Frontend & Mobile ready to test  
**Backend**: http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com

---

## 🎯 TL;DR

Everything is already configured! Just start the dev servers and test.

### Frontend Web (3 commands)
```bash
cd frontend-web
npm install
npm run dev
```
→ Open http://localhost:3004

### Mobile App (3 commands)
```bash
cd mobile-app
npm install
npx expo start
```
→ Press `i` for iOS or `a` for Android

---

## ✅ What's Already Done

### Backend (AWS ECS Fargate)
- ✅ Spring Boot application deployed and running
- ✅ PostgreSQL database with Flyway migrations
- ✅ S3 bucket for file storage
- ✅ JWT authentication configured
- ✅ CORS enabled for localhost:3004 and localhost:19006
- ✅ Health check: http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health

### Frontend Web (React + Vite)
- ✅ `.env` file created with backend URL
- ✅ TypeScript types match backend exactly
- ✅ API endpoints configured correctly
- ✅ Auth uses email + password (no username)
- ✅ Upload uses originalFilename, fileSizeBytes, mimeType

### Mobile App (React Native + Expo)
- ✅ `app.json` configured with backend URL
- ✅ `.env` file created (reference)
- ✅ TypeScript types match backend exactly
- ✅ API endpoints configured correctly
- ✅ Auth and upload match frontend implementation

---

## 🧪 Quick Test

### Option 1: Test Backend API (curl)
```bash
./test-api.sh
```

Expected output:
```
✅ PASSED: Health check is UP
✅ PASSED: User registration successful
✅ PASSED: User login successful
✅ PASSED: Upload session initiated
```

### Option 2: Test Frontend
```bash
cd frontend-web && npm run dev
```
1. Open http://localhost:3004
2. Register: test@example.com / TestPass123!
3. Upload a photo

### Option 3: Test Mobile
```bash
cd mobile-app && npx expo start
```
1. Press `i` for iOS Simulator
2. Register with email/password
3. Upload a photo from library

---

## 📊 API Contract Summary

### Authentication (No username!)
```typescript
// Register
POST /api/v1/auth/register
{ email: string, password: string }

// Login  
POST /api/v1/auth/login
{ email: string, password: string }

// Response (both)
{ token: string, userId: string, email: string }
```

### Upload (3-step process)
```typescript
// 1. Initiate
POST /api/v1/uploads/initiate
{ originalFilename: string, fileSizeBytes: number, mimeType: string }

// 2. Upload parts (direct to S3)
PUT <presignedUrl>
Body: Binary chunk

// 3. Complete
POST /api/v1/uploads/{photoId}/complete
{ parts: [{ partNumber: number, etag: string }] }
```

---

## 🔧 Configuration Files

### Frontend: `frontend-web/.env`
```env
VITE_API_BASE_URL=http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com
VITE_WS_URL=ws://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/ws
```

### Mobile: `mobile-app/app.json`
```json
"extra": {
  "apiUrl": "http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1",
  "wsUrl": "ws://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/ws"
}
```

---

## 🐛 Troubleshooting

### Frontend won't connect to backend
```bash
# Restart dev server to pick up .env changes
cd frontend-web
npm run dev
```

### Mobile won't connect to backend
```bash
# Clear Expo cache
cd mobile-app
npx expo start --clear
```

### CORS errors in browser
- Backend CORS is configured for localhost:3004
- Make sure frontend is running on port 3004
- Check: `npm run dev` should start on 3004

### Can't login/register
- Backend requires email format: test@example.com
- Password min length: 8 characters
- Check backend health: curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health

---

## 📚 Full Documentation

- **INTEGRATION_VERIFICATION.md**: Complete verification report
- **DEPLOYMENT_COMPLETE.md**: Backend deployment details
- **FULLSTACK_TESTING_PLAN.md**: Comprehensive test plan
- **BACKEND_TESTING_RESULTS.md**: API specification with examples

---

## ✨ Quick Commands Reference

```bash
# Backend health check
curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health

# Frontend dev server
cd frontend-web && npm run dev

# Mobile dev server
cd mobile-app && npx expo start

# View backend logs
aws logs tail /ecs/rapid-photo-upload-dev --follow

# List S3 uploads
aws s3 ls s3://rapid-photo-upload-dev-photos-971422717446/uploads/ --recursive

# Run backend tests
./test-api.sh
```

---

**Ready to go!** 🚀 Start with `cd frontend-web && npm run dev`
