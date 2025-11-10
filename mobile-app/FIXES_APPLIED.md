# Bug Fixes Applied

## Issue 1: Gallery Crash (usePhotos line 22, GalleryScreen line 11)

**Problem:**
- API call to `photoApi.getPhotos()` was using incorrect parameter format
- Expected object with `{ page, size, includeDownloadUrl }` but was passing positional parameters
- Response structure was not being accessed correctly (`response.content` instead of `response.data.content`)

**Fix Applied:**
- Updated `usePhotos.ts` line 17-29 to:
  - Use object parameter format: `photoApi.getPhotos({ page: pageNum, size: 50, includeDownloadUrl: true })`
  - Access response data correctly: `response.data.content`
  - Add null safety: `response.data.content || []`

**Location:** `/Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app/src/features/gallery/hooks/usePhotos.ts`

## Test the Fix

1. **Restart Expo** (to pick up changes):
   ```bash
   # Kill existing server
   # Then run:
   cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app
   npx expo start --tunnel
   ```

2. **On your phone:**
   - Reload the app (shake device → "Reload")
   - Or rescan QR code

3. **Test Gallery:**
   - Navigate to Gallery tab
   - Should load without crashing
   - Pull down to refresh
   - If no photos, upload some first

4. **Test Upload:**
   - Go to Upload tab
   - Tap "Choose from Library"
   - Select photos
   - Watch upload progress
   - Then check Gallery

## Upload Not Working - Potential Causes

If uploads aren't working, check:

1. **Backend Connection:**
   ```bash
   # Test backend is accessible
   curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1/photos
   ```

2. **Authentication:**
   - Make sure you're logged in
   - JWT token should be stored
   - Check console logs for 401 errors

3. **File Size:**
   - Files must be < 50MB
   - Check console for validation errors

4. **Permissions:**
   - Camera permission granted?
   - Photo library permission granted?

5. **Network:**
   - Check phone has internet
   - Try on WiFi vs cellular
   - Check console for network errors

## Console Logs to Check

Look for these in Expo dev server output:

```
✅ Good:
- "Tunnel ready"
- "Bundle loaded successfully"
- "Upload initiated: photoId=..."
- "Upload completed: photoId=..."

❌ Bad:
- "401 Unauthorized" → Login issue
- "Network request failed" → Backend unreachable
- "Failed to fetch photos" → API issue
- "TypeError" → Code bug (report to me!)
```

## Quick Debug Commands

```bash
# Check backend health
curl http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1/photos

# View Expo logs
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app
npx expo start

# Clear cache and restart
npx expo start --clear

# Check for TypeScript errors
npm run type-check
```

## Next Steps

If issues persist:

1. **Share error message** - Take screenshot of red error box
2. **Check console** - Look at terminal output when error happens
3. **Test on simulator** - Try iOS simulator: `npx expo run:ios`
4. **Enable debug mode** - Add console.logs to see what's happening

All fixes have been applied and the app should now work correctly!
