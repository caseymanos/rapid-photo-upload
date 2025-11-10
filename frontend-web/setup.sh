#!/bin/bash

echo "Setting up RapidPhotoUpload Frontend..."
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "Error: Node.js is not installed. Please install Node.js 18+ first."
    exit 1
fi

# Check Node version
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "Error: Node.js version 18 or higher is required (current: $(node -v))"
    exit 1
fi

echo "✓ Node.js version: $(node -v)"
echo ""

# Install dependencies
echo "Installing dependencies..."
npm install

if [ $? -ne 0 ]; then
    echo "Error: Failed to install dependencies"
    exit 1
fi

echo ""
echo "✓ Dependencies installed successfully"
echo ""

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo "Creating .env file..."
    cat > .env << EOL
# Backend API URL (default uses Vite proxy)
VITE_API_URL=http://localhost:8080
EOL
    echo "✓ .env file created"
fi

echo ""
echo "=========================================="
echo "Frontend setup complete!"
echo "=========================================="
echo ""
echo "To start the development server:"
echo "  npm run dev"
echo ""
echo "The app will be available at:"
echo "  http://localhost:3000"
echo ""
echo "Make sure the backend is running at:"
echo "  http://localhost:8080"
echo ""
