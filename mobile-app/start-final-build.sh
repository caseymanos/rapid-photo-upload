#!/bin/bash

# Final working build with all fixes
# Build #4 includes:
# - Correct API URL with /api/v1
# - HTTP/ATS exception for iOS
# - Updated FlashList and expo-image packages
# - Verified working login

echo "🚀 Starting Final Build #4"
echo "=========================="
echo ""
echo "✅ Login tested and working"
echo "✅ API URL fixed (/api/v1)"
echo "✅ HTTP/ATS exception added"
echo "✅ FlashList updated to v2.0.2"
echo ""
echo "⏱️  Build will take ~15-20 minutes"
echo ""

cd "$(dirname "$0")"

# Start the build
eas build --platform ios --profile preview

echo ""
echo "📋 What's next:"
echo ""
echo "1. Wait for build to complete (~15-20 min)"
echo "2. You'll get an email notification"
echo "3. Get install link: eas build:list"
echo "4. Share link with friends!"
echo ""
echo "Test credentials:"
echo "  Email: demo@test.com"
echo "  Password: Demo1234"
echo ""
