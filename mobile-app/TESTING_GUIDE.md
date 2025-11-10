# Mobile App Testing Guide

## Current Status
✅ All components implemented
✅ Navigation setup complete
✅ API integration configured
✅ Expo dev server running on port 8082

## Quick Start

### 1. Start the App
The Expo dev server is already running. You should see a QR code in the terminal.

**To run on simulator/emulator:**
- **iOS Simulator:** Press `i` in the Expo terminal
- **Android Emulator:** Press `a` in the Expo terminal
- **Physical Device:** Scan QR code with Expo Go app

### 2. Test Authentication Flow

**If you see the Upload/Gallery tabs immediately:**
This means you were previously logged in. Tap the **Logout** button in the top-right corner of the Upload screen.

**To test login:**
1. You should now see the Login screen
2. Register a new account:
   - Email: `test@example.com`
   - Password: `password123`
3. Or login with existing credentials

### 3. Test Photo Upload

**Upload from Library:**
1. Navigate to "Upload" tab
2. Tap "Choose from Library"
3. Grant permissions if prompted
4. Select one or more photos
5. Watch upload progress (10 concurrent uploads)
6. Each photo shows progress bar and status

**Upload from Camera:**
1. Tap "Take Photo"
2. Grant camera permission if prompted
3. Take a photo
4. Watch upload progress

**Features to test:**
- Cancel upload (tap Cancel while uploading)
- Retry failed upload (if upload fails)
- Multiple concurrent uploads (select 20+ photos)
- Upload stats display (X/Y completed)

### 4. Test Gallery

**View Photos:**
1. Navigate to "Gallery" tab
2. Photos display in a 3-column grid
3. Pull down to refresh
4. Scroll to bottom to load more (pagination)

**View Photo Details:**
1. Tap any photo in the grid
2. View full-screen photo
3. See metadata: filename, size, type, upload date
4. See tags (if any)
5. Tap X to close

### 5. Test Edge Cases

**Empty States:**
- View Gallery when no photos uploaded (should show "No photos yet")
- View Upload screen with no uploads (should show "Select photos to start")

**Error Handling:**
- Try uploading without internet connection
- Try accessing gallery offline
- Check error messages are user-friendly

**Permissions:**
- Deny camera permission → should show alert
- Deny photo library permission → should show alert

## Known Issues & Fixes Applied

### ✅ Fixed Issues:
1. **SafeAreaView deprecation** - Updated to use `react-native-safe-area-context`
2. **Gallery crash** - Fixed undefined photos array handling
3. **MediaTypeOptions deprecation** - Updated to use array format `['images']`
4. **Navigation** - Added logout button to test auth flow
5. **PhotoGrid crash** - Added default empty array for photos prop

### Current Configuration:
- **Backend URL:** `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1`
- **Concurrent Uploads:** 10
- **Chunk Size:** 5MB
- **Max File Size:** 50MB (backend limit)

## Troubleshooting

### Can't connect to backend
**Symptom:** Login fails, photos don't load
**Solution:**
- Check backend is running at the URL in `.env`
- For local backend, update `.env`:
  - iOS: `http://localhost:8080/api/v1`
  - Android: `http://10.0.2.2:8080/api/v1`
  - Physical device: `http://<your-ip>:8080/api/v1`

### Photos not uploading
**Symptom:** Upload stuck or fails immediately
**Solution:**
- Check console logs for errors
- Verify AWS S3 credentials in backend
- Check file size (<50MB)
- Check internet connection

### App crashes on photo selection
**Symptom:** App closes when selecting photos
**Solution:**
- Check permissions in iOS Settings or Android Settings
- Restart app
- Clear app cache

### Gallery is empty after upload
**Symptom:** Photos uploaded successfully but don't appear
**Solution:**
- Pull down to refresh
- Check backend logs
- Verify photos saved to S3
- Check JWT token is valid

## Development Commands

```bash
# Start Expo dev server
npm start

# Start on specific port
npx expo start --port 8082

# Clear cache and restart
npx expo start --clear

# iOS build
npx expo run:ios

# Android build
npx expo run:android

# Type check
npm run type-check

# Lint
npm run lint
```

## Next Steps

1. **Test on physical device** - Better representation of real-world performance
2. **Test with 100+ photos** - Verify concurrency and memory management
3. **Test on slow network** - Verify progress tracking and error handling
4. **Test background uploads** - Verify uploads continue when app backgrounded (requires additional config)
5. **Add analytics** - Track upload success rates, errors, usage patterns

## Architecture Notes

**Upload Flow:**
1. User selects photos → PhotoPicker
2. Photos added to upload queue → useUploadManager
3. Manager processes 10 uploads concurrently → uploadService
4. Each upload:
   - Initiates multipart upload (gets presigned URLs)
   - Splits file into 5MB chunks
   - Uploads chunks in parallel to S3
   - Completes upload with ETags
5. Progress updates displayed → UploadProgressList

**Gallery Flow:**
1. usePhotos hook fetches from backend on mount
2. Backend returns paginated results (50 per page)
3. PhotoGrid renders with FlatList (virtualized)
4. Pull-to-refresh triggers refetch
5. Scroll to bottom loads next page

**Authentication:**
1. JWT token stored in AsyncStorage
2. Token auto-loaded on app start
3. Token added to all API requests via interceptor
4. 401 response clears token and shows login

## Performance Tips

- **FlatList** handles 1000+ photos efficiently
- **Concurrent uploads** limited to 10 to avoid overwhelming device
- **Chunk size** 5MB optimal for S3 and mobile networks
- **Image caching** handled by React Native Image component
- **Token persistence** avoids re-login on app restart

## Support

If you encounter issues not covered here:
1. Check Expo dev server logs
2. Check device logs (iOS Console app or Android logcat)
3. Check backend API logs
4. Review error messages in app

Happy testing! 🚀
