#!/bin/bash

# Build script for EAS preview builds
# This creates shareable APK/IPA files for testing with friends

set -e

echo "🚀 Building RapidPhotoUpload Preview"
echo "===================================="

# Check if logged in
if ! eas whoami > /dev/null 2>&1; then
  echo "❌ Not logged into EAS. Please run: eas login"
  exit 1
fi

echo "✅ Logged in as: $(eas whoami)"

# Build for Android first (faster, no certificates needed)
echo ""
echo "📱 Building Android APK..."
echo "This will create a shareable link you can send to friends"
echo ""

eas build --platform android --profile preview --non-interactive --auto-submit=false

echo ""
echo "✅ Android build started!"
echo ""
echo "📋 Next steps:"
echo "1. Wait for the build to complete (you'll get an email)"
echo "2. Get the shareable link with: eas build:list"
echo "3. For iOS: Run './build-preview.sh ios' (requires Apple Developer account)"
echo ""
echo "💡 To check build status: eas build:list"
echo "💡 To view build details: eas build:view [BUILD_ID]"
