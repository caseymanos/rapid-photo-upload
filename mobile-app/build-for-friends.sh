#!/bin/bash

# Simple script to build and share app with friends
# Works for both Android and iOS

set -e

echo "📱 RapidPhotoUpload - Build for Friends"
echo "========================================"
echo ""

# Check login
if ! eas whoami > /dev/null 2>&1; then
  echo "❌ Not logged into EAS. Please run: eas login"
  exit 1
fi

echo "✅ Logged in as: $(eas whoami)"
echo ""

# Select platform
if [ "$1" == "android" ]; then
  PLATFORM="android"
  echo "🤖 Building Android APK..."
elif [ "$1" == "ios" ]; then
  PLATFORM="ios"
  echo "🍎 Building iOS (ad-hoc)..."
elif [ "$1" == "both" ]; then
  PLATFORM="all"
  echo "📱 Building both Android and iOS..."
else
  echo "Usage: ./build-for-friends.sh [android|ios|both]"
  echo ""
  echo "Examples:"
  echo "  ./build-for-friends.sh android    # Build Android APK"
  echo "  ./build-for-friends.sh ios        # Build iOS (requires Apple Developer)"
  echo "  ./build-for-friends.sh both       # Build both platforms"
  echo ""
  echo "First time iOS?"
  echo "  - You need Apple Developer account (\$99/year)"
  echo "  - You need device UDIDs from friends"
  echo "  - See ios-build-guide.md for details"
  exit 1
fi

echo ""
echo "🚀 Starting build..."
echo ""

# Build
eas build --platform "$PLATFORM" --profile preview

echo ""
echo "✅ Build submitted!"
echo ""
echo "📋 Next steps:"
echo "1. Wait for build (~10-20 min, you'll get email)"
echo "2. Get shareable link: eas build:list"
echo "3. Share link with friends!"
echo ""
echo "💡 Quick commands:"
echo "   eas build:list              # See all builds"
echo "   eas build:view [BUILD_ID]   # View specific build"
echo ""

if [ "$PLATFORM" == "ios" ] || [ "$PLATFORM" == "all" ]; then
  echo "📝 iOS Note:"
  echo "   For ad-hoc: Friends need to share device UDIDs"
  echo "   Add devices: eas device:create"
  echo "   See: ios-build-guide.md for TestFlight setup"
  echo ""
fi
