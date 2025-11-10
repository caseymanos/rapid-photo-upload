# Quick Start: Testing Frontend & Mobile Apps

**Backend Status**: ✅ Deployed and Running  
**Backend URL**: http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com

---

## 🚀 1-Minute Quick Start

### Test Backend First
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload
./test-api.sh
```

Expected: All tests pass ✅

---

### Test Frontend Web App
```bash
cd frontend-web
npm install   # First time only
npm run dev
```

Then open: **http://localhost:3004**

---

### Test Mobile App
```bash
cd mobile-app
npm install   # First time only
npx expo start
```

Then press:
- **`i`** for iOS Simulator
- **`a`** for Android Emulator

---

## 📋 Test Flow

### 1. Register New User
- Email: `test1@example.com`
- Password: `TestPassword123!`

### 2. Login
- Use same credentials

### 3. Upload Photo
- Select a photo (or drag-and-drop on web)
- Monitor progress
- Verify completion

### 4. Check Backend
```bash
# View logs
aws logs tail /ecs/rapid-photo-upload-dev --follow --since 5m

# List uploaded files
aws s3 ls s3://rapid-photo-upload-dev-photos-971422717446/uploads/ --recursive
```

---

## ✅ What Should Work

- ✅ User registration (email + password)
- ✅ User login with JWT token
- ✅ File upload initiation
- ✅ Presigned S3 URL generation
- ✅ Multipart upload progress
- ✅ Upload completion
- ✅ Photo gallery display

---

## 🐛 If Something Doesn't Work

### Frontend Issues
1. Check browser console for errors
2. Verify `.env` file exists: `ls frontend-web/.env`
3. Restart dev server: `Ctrl+C`, then `npm run dev`
4. Check Network tab for API calls

### Mobile Issues
1. Check Expo logs for errors
2. Verify `app.json` has `extra.apiUrl` section
3. Restart Expo: `npx expo start --clear`
4. Try on different simulator/device

### Backend Issues
1. Check health: `curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/actuator/health`
2. View logs: `aws logs tail /ecs/rapid-photo-upload-dev --follow`

---

## 📚 More Details

- **INTEGRATION_COMPLETE.md**: Full integration documentation
- **BACKEND_TESTING_RESULTS.md**: API specification
- **FULLSTACK_TESTING_PLAN.md**: Comprehensive test plan

