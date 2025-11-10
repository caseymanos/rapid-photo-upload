#!/bin/bash

echo "========================================"
echo "RapidPhotoUpload - Build for Phone"
echo "========================================"
echo ""

# Change to mobile app directory
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/mobile-app

echo "Choose your phone platform:"
echo "1) Android (APK - easiest, works on any Android)"
echo "2) iOS (requires Apple Developer account)"
echo ""
read -p "Enter choice (1 or 2): " choice

if [ "$choice" = "1" ]; then
    echo ""
    echo "Building Android APK..."
    echo "This will take 10-20 minutes."
    echo ""
    echo "You'll be asked to:"
    echo "  1. Generate a new Android keystore (say YES)"
    echo "  2. Create a new keystore automatically (say YES)"
    echo ""
    echo "Starting build in 5 seconds..."
    sleep 5

    eas build --profile preview --platform android

    echo ""
    echo "✅ Build complete!"
    echo "You'll receive a link to download the APK."
    echo "Open the link on your Android phone to install."

elif [ "$choice" = "2" ]; then
    echo ""
    echo "Building iOS app..."
    echo "This will take 15-25 minutes."
    echo ""
    echo "You'll be asked to:"
    echo "  1. Log in to your Apple account"
    echo "  2. Select/create a provisioning profile"
    echo ""
    echo "Requirements:"
    echo "  - Apple Developer account ($99/year)"
    echo "  - Or use Expo's ad-hoc distribution (free for testing)"
    echo ""
    echo "Starting build in 5 seconds..."
    sleep 5

    eas build --profile preview --platform ios

    echo ""
    echo "✅ Build complete!"
    echo "You'll receive a link to download the .ipa file."
    echo "Install using TestFlight or directly on your device."

else
    echo "Invalid choice. Please run again and choose 1 or 2."
    exit 1
fi

echo ""
echo "========================================"
echo "What happens next:"
echo "========================================"
echo "1. EAS will build your app in the cloud"
echo "2. You'll get an email when it's ready"
echo "3. Click the link to download"
echo "4. Install on your phone"
echo ""
echo "Check build status:"
echo "  eas build:list"
echo ""
echo "Or visit: https://expo.dev/accounts/cmanos18/projects/rapid-photo-upload/builds"
