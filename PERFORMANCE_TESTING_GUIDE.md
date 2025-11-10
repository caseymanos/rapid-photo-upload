# Performance Testing Guide

Quick guide to verify and measure the performance optimizations.

## Prerequisites

1. Backend running on localhost:8080 (or deployed)
2. Frontend running on localhost:5173 (or deployed)
3. Browser DevTools console open
4. Sample images ready (recommend 10-20 images, 5-15MB each)

## Testing the Optimizations

### 1. Upload Performance Test

**Steps**:
1. Open browser DevTools Console (F12)
2. Navigate to Upload page
3. Select 10 images (drag & drop or file picker)
4. Watch console for performance logs

**Expected Console Output**:
```
[Performance] Upload complete for photo1.jpg (12.5MB):
  Total: 3200ms
  Initiate: 250ms
  Upload: 2750ms (36.4 Mbps)
  Complete: 200ms

[Performance] Upload complete for photo2.jpg (8.3MB):
  Total: 2100ms
  Initiate: 180ms
  Upload: 1750ms (37.9 Mbps)
  Complete: 170ms
...
```

**What to Verify**:
- ✅ Multiple uploads running concurrently (not sequential)
- ✅ Initiate phase < 500ms per file
- ✅ Upload throughput > 20 Mbps (depends on connection)
- ✅ Complete phase < 300ms per file
- ✅ 10 files complete in ~10-15s (not 100s+)

### 2. Gallery Preloading Test

**Steps**:
1. Clear browser cache
2. Start uploading 10+ images
3. Wait ~1 second after uploads start
4. Check Network tab in DevTools

**Expected Behavior**:
```
[Performance] Gallery preloaded in 1834.23ms
```

**What to Verify**:
- ✅ `/api/v1/photos` request made automatically during upload
- ✅ Preload happens ~1 second after uploads start
- ✅ Gallery navigation is instant (no loading spinner)

### 3. Gallery Load Performance Test

**Steps**:
1. Backend logs should show timing
2. Navigate to Gallery page
3. Watch console for performance logs

**Expected Console Output**:
```
[Performance] Fetched 45 photos in 1823.45ms
```

**Backend Logs**:
```
INFO  - Retrieved 45 photos in 1847ms (parallel presigned URL generation)
```

**What to Verify**:
- ✅ Gallery loads in < 3s for 50 photos
- ✅ Gallery loads in < 5s for 100 photos
- ✅ Backend uses parallel URL generation (check logs)

### 4. API Timeout Test

**Steps**:
1. Simulate slow network in DevTools (Network → Throttling → Slow 3G)
2. Try to upload or fetch gallery
3. Watch for timeout behavior

**Expected Behavior**:
- Requests timeout after 15s (not 30s)
- Error message appears quickly

**What to Verify**:
- ✅ Timeout happens at 15s, not 30s
- ✅ Error feedback is immediate
- ✅ No hanging requests

### 5. Slow Request Detection Test

**Steps**:
1. Monitor console during normal usage
2. Look for slow request warnings

**Example Output** (if API is slow):
```
[Performance] Slow API request: /api/v1/photos took 3452ms
```

**What to Verify**:
- ✅ Requests > 2s are logged as warnings
- ✅ You can identify bottlenecks easily

## Backend Performance Verification

### Check Backend Logs

Look for these patterns in Spring Boot logs:

**Presigned URL Generation** (should show parallel):
```
INFO  - Generating 20 presigned URLs for key: users/123/photo.jpg
INFO  - Generated 20 presigned URLs in 387ms (parallel)
```

**Photo Queries** (should show timing):
```
INFO  - Retrieved 100 photos in 2134ms (parallel presigned URL generation)
```

### Expected Timings

| Operation | Before | After | Improvement |
|-----------|--------|-------|-------------|
| Generate 20 presigned URLs | ~2000ms | ~400ms | 5x faster |
| Fetch 100 photos | ~10000ms | ~2000ms | 5x faster |
| Upload 10 files (web) | ~100s | ~10s | 10x faster |
| Navigate to gallery | ~2s wait | instant | Preloaded |

## Mobile App Testing

Same tests apply for React Native mobile app:

**Steps**:
1. Run app on device or simulator
2. Open React Native debugger or logs
3. Upload photos from device
4. Check performance logs

**Expected Mobile Logs**:
```
[Performance] Upload complete for IMG_1234.jpg (15.3MB):
  Total: 3421ms
  Initiate: 234ms
  Upload: 2987ms (41.2 Mbps)
  Complete: 200ms
```

## Load Testing (Optional)

For more rigorous testing, use these tools:

### Artillery Load Test

Create `load-test.yml`:
```yaml
config:
  target: 'http://localhost:8080'
  phases:
    - duration: 60
      arrivalRate: 10
      name: "Sustained load"

scenarios:
  - name: "Upload workflow"
    flow:
      - post:
          url: "/api/v1/uploads/initiate"
          json:
            originalFilename: "test.jpg"
            fileSizeBytes: 5242880
            mimeType: "image/jpeg"
```

Run: `artillery run load-test.yml`

### Browser Performance API

Use Performance API for detailed metrics:

```javascript
// In browser console
performance.getEntriesByType('resource')
  .filter(r => r.name.includes('/api/'))
  .forEach(r => console.log(`${r.name}: ${r.duration}ms`));
```

## Troubleshooting

### Uploads Still Slow?

**Check**:
1. Network connection speed (run speedtest.net)
2. Backend is actually running (check health endpoint)
3. Browser console for errors
4. Backend logs for errors or circuit breaker trips

### Gallery Not Preloading?

**Check**:
1. Console for "Gallery preloaded" message
2. Network tab shows /api/v1/photos request
3. Upload is actually in progress (not just pending)
4. No errors in console

### No Performance Logs?

**Check**:
1. Browser console is open
2. Console level set to "Info" or "All" (not just Errors)
3. Frontend code is latest version
4. Backend logging level is INFO (check application.yml)

## Success Criteria

Optimizations are working correctly if:

✅ **Backend**: Parallel stream processing evident in logs  
✅ **Frontend**: Upload concurrency working (multiple uploads simultaneously)  
✅ **Preloading**: Gallery loads instantly after uploads  
✅ **Instrumentation**: Detailed performance logs in console and backend  
✅ **Timeouts**: API requests fail fast (15s, not 30s)  
✅ **Overall**: 10x faster end-to-end experience  

## Performance Benchmarks

Record your results:

| Metric | Your Result | Target |
|--------|-------------|--------|
| Upload 10 files (5MB each) | _____s | <15s |
| Generate 20 presigned URLs | _____ms | <500ms |
| Fetch 50 photos | _____ms | <3000ms |
| Gallery preload | _____ms | <2000ms |
| Navigate to preloaded gallery | _____ms | <500ms |

## Next Steps

After verifying performance:

1. **Monitor in production**: Set up application performance monitoring (APM)
2. **Set up alerts**: Alert on slow requests (>5s)
3. **Regular testing**: Run these tests weekly
4. **Optimize further**: Use profiling tools for additional gains

## Additional Resources

- [Web Vitals Chrome Extension](https://chrome.google.com/webstore/detail/web-vitals/)
- [React DevTools Profiler](https://react.dev/learn/react-developer-tools)
- [Artillery Load Testing](https://www.artillery.io/docs)
- [Java Flight Recorder](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm)
