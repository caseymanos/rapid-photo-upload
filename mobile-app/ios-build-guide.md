# iOS Build & Distribution Guide

## Prerequisites

You need an **Apple Developer account** ($99/year) to distribute iOS builds to friends.

**Two options for sharing with friends:**

### Option 1: TestFlight (Recommended - Easier)
- ✅ Up to 10,000 testers
- ✅ Easy installation via TestFlight app
- ✅ No device UDIDs needed
- ✅ Automatic updates
- ❌ Requires App Store Connect setup
- ❌ 24-48 hour review for external testers

### Option 2: Ad-Hoc Distribution (Internal)
- ✅ Instant sharing (no review)
- ✅ Direct install from link
- ❌ Limited to 100 devices per year
- ❌ Need device UDIDs upfront
- ❌ Must rebuild for new devices

## Quick Start: iOS Build

### Step 1: Build with EAS (First Time)

```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app

# Build for internal distribution (ad-hoc)
eas build --platform ios --profile preview
```

EAS will ask:
1. **Generate credentials?** → Yes ✅
2. **Apple ID** → Enter your Apple Developer account email
3. **Apple ID password** → Enter your password
4. **2FA code** → Enter code from your device

EAS automatically:
- Creates certificates
- Creates provisioning profiles
- Stores credentials securely
- Builds your app

### Step 2: For Ad-Hoc Distribution (Internal)

You need device UDIDs from friends' iPhones.

**Get Device UDID:**
Friends can use one of these methods:
- **Method A**: Open https://expo.dev/register-device on their iPhone
- **Method B**: Connect to Mac → Finder → Device → Click on info
- **Method C**: Install app like "UDID+" from App Store

**Add devices to EAS:**
```bash
eas device:create
```

Then rebuild:
```bash
eas build --platform ios --profile preview
```

### Step 3: For TestFlight Distribution (Recommended)

Update your build profile for App Store distribution:

```bash
# Build for TestFlight
eas build --platform ios --profile production
```

After build completes:
```bash
# Submit to App Store Connect
eas submit --platform ios
```

Then in App Store Connect:
1. Go to TestFlight tab
2. Add external testers
3. Send invites (they install via TestFlight app)

## Current Configuration

Your app is configured with:
- **Bundle ID**: `com.rapidphotoupload.app`
- **App Name**: RapidPhotoUpload
- **Distribution**: Internal (preview profile)
- **Backend API**: `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1`

## Commands Reference

```bash
# Build iOS preview (ad-hoc)
eas build --platform ios --profile preview

# Build iOS for TestFlight
eas build --platform ios --profile production

# Submit to App Store Connect
eas submit --platform ios

# Manage devices
eas device:list
eas device:create
eas device:delete

# Manage credentials
eas credentials

# Check build status
eas build:list

# View specific build
eas build:view [BUILD_ID]
```

## Recommended Workflow for Friends

**Easiest: TestFlight**
1. Build with `production` profile
2. Submit to App Store Connect with `eas submit`
3. Invite friends in TestFlight
4. Friends install TestFlight app → click invite link → install your app
5. Push updates → friends auto-update

**Fastest: Ad-Hoc**
1. Get device UDIDs from friends (up to 100)
2. Register devices with `eas device:create`
3. Build with `preview` profile
4. Share download link
5. Friends install directly from link

## Cost Comparison

| Method | Apple Cost | EAS Cost | Device Limit | Review Time |
|--------|-----------|----------|--------------|-------------|
| TestFlight | $99/year | Free tier OK | 10,000 | 24-48 hours (first time) |
| Ad-Hoc | $99/year | Free tier OK | 100/year | None |

## Tips

1. **First build**: Use ad-hoc (preview) for instant testing
2. **Long-term**: Set up TestFlight for easier distribution
3. **Updates**: TestFlight auto-updates; ad-hoc requires new link
4. **Device limit**: Track carefully with ad-hoc (only 100/year)

## Build Times

- iOS builds: ~15-20 minutes
- Includes compilation, signing, and upload
- You'll receive email when complete

## Next Steps

1. **Get Apple Developer account** (if you don't have one): https://developer.apple.com/programs/
2. **Run first iOS build**: `eas build --platform ios --profile preview`
3. **Collect friend device UDIDs** for ad-hoc, OR
4. **Set up TestFlight** for easier long-term sharing

## Troubleshooting

### "No valid code signing identity found"
Run: `eas credentials` → Select iOS → Regenerate certificates

### "Device not in provisioning profile"
Add device: `eas device:create` → Rebuild

### "Apple ID authentication failed"
Check 2FA is enabled, use app-specific password if needed

---

**Ready to build?** Run:
```bash
eas build --platform ios --profile preview
```
