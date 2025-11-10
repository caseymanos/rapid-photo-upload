# iOS HTTP Backend Fix

## Problem

iOS App Transport Security (ATS) blocks HTTP connections by default. Your app was getting:
```
Error Domain=NSURLErrorDomain Code=-1022
"The resource could not be loaded because the App Transport Security policy
requires the use of a secure connection."
```

## Solution Applied ✅

Added ATS exception in `app.config.js` to allow HTTP for your backend domain:

```javascript
NSAppTransportSecurity: {
  NSAllowsArbitraryLoads: false,
  NSExceptionDomains: {
    "rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com": {
      NSTemporaryExceptionAllowsInsecureHTTPLoads: true,
      NSIncludesSubdomains: true,
      NSTemporaryExceptionMinimumTLSVersion: "TLSv1.0"
    }
  }
}
```

This allows HTTP **only** for your specific backend domain while keeping HTTPS required for everything else.

## Testing the Fix

### Option 1: Expo Go (Quick Test)
```bash
# Kill and restart the dev server
npx expo start --clear
```

Then:
1. Scan QR code with your iPhone
2. Try logging in
3. Should work now! ✅

### Option 2: Development Build (More Reliable)
```bash
# Build a development client that includes the ATS fix
eas build --platform ios --profile development
```

Install the development build on your device, then you can use Fast Refresh for quick iterations.

### Option 3: Preview/Production Build
```bash
# Full build with ATS fix
eas build --platform ios --profile preview
```

## ⚠️ Important: Production Considerations

### This is a Development Workaround
Using HTTP is **NOT recommended for production** because:
- ❌ Data transmitted in plain text (including passwords!)
- ❌ Vulnerable to man-in-the-middle attacks
- ❌ Apple may reject App Store submissions using HTTP
- ❌ Users will see security warnings

### Production Solution: Add HTTPS

You need to add HTTPS to your backend. Here are options:

#### Option A: AWS Application Load Balancer with ACM (Recommended)
```bash
# 1. Request SSL certificate in AWS Certificate Manager
aws acm request-certificate \
  --domain-name rapidphotoupload.yourdomain.com \
  --validation-method DNS

# 2. Add DNS validation records
# 3. Update ALB to use HTTPS listener with certificate
# 4. Update mobile app to use https://rapidphotoupload.yourdomain.com
```

#### Option B: CloudFront with SSL
- Add CloudFront distribution in front of ALB
- Use free AWS certificate
- Update app to use CloudFront domain

#### Option C: nginx Reverse Proxy
- Add nginx with Let's Encrypt certificate
- Proxy requests to your backend
- Update app to use nginx domain

### After HTTPS Setup

1. Update your `eas.json`:
```json
{
  "build": {
    "preview": {
      "env": {
        "API_BASE_URL": "https://your-domain.com/api/v1",
        "WS_URL": "wss://your-domain.com/ws"
      }
    }
  }
}
```

2. Remove the ATS exception from `app.config.js`

3. Rebuild your app

## Current Configuration Status

✅ **Development/Testing**: HTTP allowed for your backend
⚠️ **Production**: Needs HTTPS before App Store submission

## Quick Commands

```bash
# Restart Expo with changes
npx expo start --clear

# Build new development version
eas build --platform ios --profile development

# Build new preview version
eas build --platform ios --profile preview

# Check current config
cat app.config.js | grep -A 10 NSAppTransportSecurity
```

## Next Steps

For now (testing):
1. ✅ ATS exception is configured
2. Restart your Expo dev server
3. Reload app on iOS
4. Login should work!

For production:
1. Set up HTTPS on your backend
2. Update environment variables
3. Remove ATS exception
4. Rebuild app
5. Submit to App Store

---

**Note**: The WebSocket connection also needs to use `wss://` (secure WebSocket) when you move to HTTPS.
