# Frontend & Mobile Testing Guide

**Date**: 2025-11-09 15:55 PST
**Backend Status**: ✅ Running (2 healthy tasks)
**Backend URL**: `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com`

---

## ✅ Backend Verification (Already Done)

```bash
# Health check
curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health
# Response: {"status":"UP"}

# ECS Service
# RunningCount: 2, PendingCount: 0
```

---

## 🌐 Frontend Web Testing (In Progress)

### Status: Dev Server Running
- **URL**: http://localhost:3004/
- **Server**: Vite (running in background)
- **Configuration**: `.env` with backend URL ✅

### Test Workflow (15 minutes)

1. **Open Browser**
   ```
   Open: http://localhost:3004/
   ```

2. **Register New User**
   - Click "Register" or navigate to registration
   - Email: `test-frontend@example.com`
   - Password: `TestPass123!`
   - Submit
   - **Expected**: Redirected to login or dashboard with JWT token stored

3. **Login**
   - Email: `test-frontend@example.com`
   - Password: `TestPass123!`
   - **Expected**: Successful login, JWT token in localStorage

4. **Upload Photos (Single)**
   - Click "Upload" or drag & drop
   - Select 1 test image (e.g., screenshot, photo)
   - **Expected**: 
     - Progress bar appears
     - Upload completes
     - Photo appears in gallery

5. **Upload Photos (Batch)**
   - Select 5-10 images
   - **Expected**:
     - All uploads start
     - Progress tracking for each
     - All complete successfully
     - All photos appear in gallery

6. **View Gallery**
   - Navigate to photos/gallery page
   - **Expected**: All uploaded photos displayed

7. **Error Handling**
   - Try uploading invalid file type (e.g., `.txt`)
   - **Expected**: Error message displayed
   - Try uploading without login
   - **Expected**: Redirected to login

### Browser Console Checks
- Open DevTools (F12)
- Check Console tab for:
  - ✅ No CORS errors
  - ✅ No API errors
  - ✅ Successful API responses (200/201/204)
- Check Network tab:
  - ✅ POST to `/api/v1/auth/register` → 200
  - ✅ POST to `/api/v1/auth/login` → 200
  - ✅ POST to `/api/v1/uploads/initiate` → 200
  - ✅ PUT to S3 presigned URL → 200
  - ✅ POST to `/api/v1/uploads/{id}/complete` → 200/204
  - ✅ GET to `/api/v1/photos` → 200

### Stop Frontend Server (When Done)
```bash
# Get process ID
ps aux | grep "vite"

# Kill the process
kill <PID>

# Or use pkill
pkill -f "vite"
```

---

## 📱 Mobile App Testing (Next)

### Prerequisites
- Expo Go app installed on physical device (iOS/Android)
- OR iOS Simulator / Android Emulator installed

### Start Mobile Dev Server

```bash
cd mobile-app
npm install
npx expo start
```

### Test Workflow (15 minutes)

1. **Launch App**
   - Scan QR code with Expo Go (physical device)
   - OR press `i` for iOS simulator
   - OR press `a` for Android emulator

2. **Register New User**
   - Email: `test-mobile@example.com`
   - Password: `TestPass123!`
   - **Expected**: Account created, logged in

3. **Upload Photos**
   - Tap "Select Photos" or equivalent
   - Choose 3-5 photos from device/simulator
   - **Expected**:
     - Photos selected
     - Upload starts
     - Progress shown for each
     - All uploads complete

4. **View Gallery**
   - Navigate to gallery/photos screen
   - **Expected**: All uploaded photos displayed

5. **Offline Handling** (Optional)
   - Enable airplane mode
   - Try uploading
   - **Expected**: Error message or queuing behavior
   - Disable airplane mode
   - **Expected**: Retry or automatic upload

### Expo Dev Tools Checks
- In terminal, check for:
  - ✅ No errors/warnings
  - ✅ Successful API requests
- In app logs:
  - ✅ No network errors
  - ✅ No authentication errors

---

## 🧪 End-to-End Integration Test

### Cross-Platform Verification

1. **Upload from Frontend**
   - Login as `test-frontend@example.com`
   - Upload 3 photos

2. **View from Mobile**
   - Login as `test-frontend@example.com` (same account)
   - **Expected**: See same 3 photos in gallery

3. **Upload from Mobile**
   - Upload 2 more photos

4. **View from Frontend**
   - Refresh gallery
   - **Expected**: See all 5 photos

### Multi-User Test

1. **Register 2nd user on frontend**: `user2@example.com`
2. **Upload photos as user2**
3. **Login as user1 on mobile**
4. **Expected**: Only see user1's photos (isolation verified)

---

## 📊 Performance Testing

### High-Volume Upload (Frontend)

```bash
# Create 50 test images (use actual photos or generate)
# Upload all 50 simultaneously
# Expected:
# - All uploads initiate
# - Progress tracking accurate
# - No failures
# - All complete within reasonable time (~2-5 min for 50 photos)
```

### Monitor Backend During Upload
```bash
# In separate terminal
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Watch for:
# - No errors
# - Upload initiation logs
# - Completion logs
# - No 500 errors
```

---

## ✅ Success Criteria Checklist

### Frontend
- [ ] Registration works
- [ ] Login works
- [ ] Single photo upload works
- [ ] Batch photo upload works (5-10 photos)
- [ ] Progress tracking accurate
- [ ] Gallery displays photos
- [ ] Error handling works
- [ ] No CORS errors
- [ ] No API errors

### Mobile
- [ ] Registration works
- [ ] Login works
- [ ] Photo selection works
- [ ] Single photo upload works
- [ ] Multiple photo upload works
- [ ] Progress tracking accurate
- [ ] Gallery displays photos
- [ ] Error handling works
- [ ] No network errors

### Integration
- [ ] Photos uploaded from web visible in mobile (same user)
- [ ] Photos uploaded from mobile visible in web (same user)
- [ ] User isolation working (different users see different photos)
- [ ] High-volume upload works (50+ photos)

---

## 🐛 Common Issues & Fixes

### Frontend: CORS Errors
**Symptom**: Browser console shows CORS policy error
**Fix**: Backend already configured for `localhost:3004`, should work
**Verify**: Check `SecurityConfig.java` CORS origins

### Frontend: 401 Unauthorized
**Symptom**: API calls return 401
**Cause**: JWT token expired (24h) or missing
**Fix**: Re-login to get fresh token

### Mobile: Network Request Failed
**Symptom**: Upload fails with network error
**Cause**: Device can't reach AWS ALB (firewall, VPN, etc.)
**Fix**: 
- Check device internet connection
- Verify ALB URL accessible from device
- Try from simulator first (uses same network as host)

### Mobile: Expo Start Fails
**Symptom**: `npx expo start` errors
**Fix**:
```bash
cd mobile-app
rm -rf node_modules package-lock.json
npm install
npx expo install --fix
npx expo start --clear
```

### Upload Fails: Presigned URL Expired
**Symptom**: S3 PUT returns 403
**Cause**: Presigned URL expired (2h)
**Fix**: Re-initiate upload to get fresh URLs

### Photos Not Appearing in Gallery
**Symptom**: Upload completes but gallery empty
**Cause**: Upload completion not called or failed
**Fix**: Check browser/app console for errors, verify `/uploads/{id}/complete` was called

---

## 📈 Monitoring During Testing

### CloudWatch Dashboard
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=rapid-photo-upload-dev

**Watch for**:
- Request count increasing during uploads
- Response time staying low (<500ms)
- No 5xx errors
- ECS CPU/memory within limits

### Application Logs
```bash
# Tail logs
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Filter for errors only
aws logs tail /ecs/rapid-photo-upload-dev --follow --filter-pattern "ERROR"
```

### ECS Service Health
```bash
aws ecs describe-services \
  --cluster rapid-photo-upload-dev \
  --services rapid-photo-upload-dev \
  --query 'services[0].{RunningCount:runningCount, DesiredCount:desiredCount}'
```

---

## 🚀 Next Steps After Testing

### If All Tests Pass ✅
1. Deploy frontend to CloudFront (Phase 3)
2. Build mobile app via EAS (Phase 4)
3. Production readiness checklist
4. Deploy to production environment

### If Tests Fail ❌
1. Check CloudWatch logs for backend errors
2. Check browser/app console for frontend errors
3. Verify API request/response in Network tab
4. Re-run backend tests to verify backend still healthy
5. Report issue with:
   - Error message
   - Steps to reproduce
   - Browser/device info
   - Network tab screenshot

---

## 📞 Quick Reference

**Backend URL**: `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com`
**Frontend URL**: `http://localhost:3004/`
**API Prefix**: `/api/v1`

**Test Credentials**:
- Frontend: `test-frontend@example.com` / `TestPass123!`
- Mobile: `test-mobile@example.com` / `TestPass123!`

**Backend Health**: `curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health`

---

Good luck with testing! 🚀
