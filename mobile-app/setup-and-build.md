# EAS Build Setup Guide

## First-Time Setup (One-time only)

Since this is the first build, you need to generate credentials interactively:

### Step 1: Generate Android Credentials
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app
eas credentials
```
Select:
- Platform: Android
- Action: "Set up or manage credentials"
- Choose: "Generate a new keystore"

This creates the signing keys needed for Android builds.

### Step 2: Build Android Preview
```bash
eas build --platform android --profile preview
```

This will:
- Build your app in the cloud
- Generate a shareable APK
- Provide a QR code and download link
- Send you an email when complete

### Step 3: Share with Friends

After the build completes (~10-15 minutes):

```bash
eas build:list
```

Copy the install URL and share it! Friends can:
- Open the link on their Android phone
- Download and install the APK
- Test your app immediately

## For iOS Builds (Requires Apple Developer Account)

```bash
eas build --platform ios --profile preview
```

You'll need:
- Apple Developer account ($99/year)
- Provisioning profile and certificates
- EAS will guide you through setup

## Continuous Updates

After initial setup, future builds are simple:

```bash
# Build Android
eas build --platform android --profile preview

# Build iOS
eas build --platform ios --profile preview

# Build both
eas build --platform all --profile preview
```

## Quick Commands

```bash
# Check build status
eas build:list

# View specific build
eas build:view [BUILD_ID]

# Cancel a build
eas build:cancel [BUILD_ID]

# Check who you're logged in as
eas whoami
```

## What's Configured

✅ EAS project: `e89de57f-302a-48f7-88a1-1a211028f07f`
✅ Android package: `com.rapidphotoupload.app`
✅ iOS bundle: `com.rapidphotoupload.app`
✅ App icons and splash screens
✅ Build profiles (development, preview, production)
✅ Environment variables for backend API

## Backend Connection

Your app is configured to connect to:
- API: `http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1`
- WebSocket: `ws://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/ws`

## Next Steps

1. **Run the credentials setup** (see Step 1 above)
2. **Trigger your first build** (see Step 2 above)
3. **Share the link** with friends once complete
4. **Make changes** to your code
5. **Rebuild** with the same command - EAS handles versioning automatically!

---

## Troubleshooting

### "Input is required" Error
You need to run the credentials command interactively (not in non-interactive mode).
Run: `eas credentials` and follow the prompts.

### Build Takes Too Long
Builds typically take 10-15 minutes. You can close the terminal - you'll get an email when done.

### Need to Update Backend URL
Edit `eas.json` and change the `API_BASE_URL` in the preview profile, then rebuild.
