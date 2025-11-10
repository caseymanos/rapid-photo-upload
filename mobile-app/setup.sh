#!/bin/bash

# RapidPhotoUpload Mobile App - Setup Script

set -e

echo "🚀 RapidPhotoUpload Mobile App Setup"
echo "===================================="
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18+ first."
    exit 1
fi

echo "✅ Node.js version: $(node --version)"
echo ""

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed. Please install npm first."
    exit 1
fi

echo "✅ npm version: $(npm --version)"
echo ""

# Install dependencies
echo "📦 Installing dependencies..."
npm install

echo ""
echo "✅ Dependencies installed successfully!"
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
    echo "✅ .env file created!"
    echo ""
    echo "⚠️  IMPORTANT: Edit .env and set your API_URL:"
    echo "   - iOS Simulator: http://localhost:8080/api/v1"
    echo "   - Android Emulator: http://10.0.2.2:8080/api/v1"
    echo "   - Physical Device: http://<your-computer-ip>:8080/api/v1"
    echo ""
else
    echo "✅ .env file already exists"
    echo ""
fi

echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit .env and configure API_URL"
echo "2. Ensure backend is running (http://localhost:8080)"
echo "3. Run 'npm start' to start Expo"
echo "4. Scan QR code with Expo Go app or press 'i' for iOS, 'a' for Android"
echo ""
echo "Documentation:"
echo "- README.md - Complete implementation guide"
echo "- MOBILE_COMPLETE.md - Implementation summary"
echo ""
echo "Happy coding! 🎉"
