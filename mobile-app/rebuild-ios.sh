#!/bin/bash

# Rebuild iOS with HTTP fix

echo "🔧 Rebuilding iOS app with HTTP/ATS fix"
echo "========================================"
echo ""
echo "This build includes the App Transport Security exception"
echo "so your app can connect to the HTTP backend."
echo ""
echo "⏱️  Build will take ~15-20 minutes"
echo ""

cd "$(dirname "$0")"

# Start the build
eas build --platform ios --profile preview

echo ""
echo "✅ Build started!"
echo ""
echo "📋 While you wait:"
echo "  1. You'll get an email when done"
echo "  2. Or check status: eas build:list"
echo ""
echo "📱 After build completes:"
echo "  1. Get the install URL: eas build:list"
echo "  2. Open URL on your iPhone"
echo "  3. Install and test login"
echo ""
echo "💡 Tip: Test in Expo Go first for instant feedback:"
echo "  npx expo start --clear"
echo ""
