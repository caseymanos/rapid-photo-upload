# Mobile App Deployment Guide

## Quick Deploy to Your Phone

### Option 1: Expo Go (Easiest - 2 minutes)

**What you need:**
- Expo Go app on your phone ([iOS](https://apps.apple.com/app/expo-go/id982107779) | [Android](https://play.google.com/store/apps/details?id=host.exp.exponent))
- Your phone and computer on the same WiFi network

**Steps:**

1. **In your terminal (in the mobile-app directory):**
   ```bash
   npx expo start
   ```

2. **On your phone:**
   - Open Expo Go app
   - Tap "Scan QR code"
   - Scan the QR code shown in your terminal
   - App will load instantly!

**For remote access (not on same WiFi):**
```bash
npx expo start --tunnel
```
This creates a public URL you can access from anywhere.

---

### Option 2: EAS Build (Production-Ready)

**What you need:**
- Expo account (you're logged in as: cmanos18)
- 15-20 minutes build time

**For iOS (TestFlight or Direct Install):**

```bash
# Development build (install directly on device)
eas build --profile preview --platform ios

# This will:
# 1. Build the app on Expo servers
# 2. Give you a download link
# 3. Install via link on your iPhone
```

**For Android (APK - Direct Install):**

```bash
# Development build (install directly)
eas build --profile preview --platform android

# This will:
# 1. Build APK on Expo servers
# 2. Give you download link
# 3. Install APK on Android device
```

**After build completes:**
1. You'll get a link like: `https://expo.dev/accounts/cmanos18/projects/rapid-photo-upload/builds/xxx`
2. Open link on your phone
3. Download and install

---

## Over-The-Air (OTA) Updates

Once you have the app installed (via Option 2), you can push updates without rebuilding:

### Publish Update:

```bash
# Publish to preview channel
eas update --branch preview --message "Fix photo upload bug"

# Users get update automatically next time they open app!
```

### Check for updates in app:
The app checks for updates on launch. No user action needed!

---

## Current Configuration

**Project ID:** `e89de57f-302a-48f7-88a1-1a211028f07f`
**Expo Account:** `cmanos18`
**Project URL:** https://expo.dev/accounts/cmanos18/projects/rapid-photo-upload

**API Backend:** `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1`

---

## Recommended Flow

For fastest deployment **right now**:

1. **Use Expo Go** (Option 1)
   - Get app on phone in 2 minutes
   - Test all features
   - Iterate quickly

2. **Build standalone app** (Option 2) when ready for:
   - Production deployment
   - App Store / Play Store
   - TestFlight beta testing
   - Sharing with non-developers

---

## Commands Cheat Sheet

```bash
# Start dev server (same WiFi)
npx expo start

# Start with tunnel (remote access)
npx expo start --tunnel

# Build for iOS (development)
eas build --profile development --platform ios

# Build for Android (APK)
eas build --profile preview --platform android

# Publish OTA update
eas update --branch preview

# Check build status
eas build:list

# View logs
eas build:view [build-id]
```

---

## Troubleshooting

### Expo Go shows "Something went wrong"
**Solution:**
- Check that backend is running
- Verify API_BASE_URL in .env
- Check console for errors

### Can't scan QR code
**Solution:**
- Use tunnel mode: `npx expo start --tunnel`
- Or manually enter URL in Expo Go

### Build fails on EAS
**Solution:**
- Check eas.json configuration
- Verify all dependencies are installed
- Check build logs: `eas build:list`

### OTA update not showing
**Solution:**
- Updates apply on app restart
- Check update branch matches your build
- Verify using: `eas update:list`

---

## Next Steps

1. **Deploy to TestFlight (iOS):**
   ```bash
   eas build --profile production --platform ios
   eas submit -p ios
   ```

2. **Deploy to Play Store (Android):**
   ```bash
   eas build --profile production --platform android
   eas submit -p android
   ```

3. **Set up CI/CD:**
   - Use GitHub Actions
   - Auto-publish on merge to main
   - Auto-submit to stores

---

## Support

- **Expo Docs:** https://docs.expo.dev
- **EAS Build:** https://docs.expo.dev/build/introduction/
- **EAS Update:** https://docs.expo.dev/eas-update/introduction/
- **Your Project:** https://expo.dev/accounts/cmanos18/projects/rapid-photo-upload
