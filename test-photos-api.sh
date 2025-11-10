#!/bin/bash

BASE_URL="http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com"

echo "Testing Photos API with downloadUrl field..."
echo "============================================"
echo ""

# Register a new user
TIMESTAMP=$(date +%s)
EMAIL="test_$TIMESTAMP@example.com"
PASSWORD="TestPassword123!"

echo "1. Registering user: $EMAIL"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
echo "Registration response: $REGISTER_RESPONSE"
echo ""

# Try to login
echo "2. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ Login failed. Response:"
    echo "$LOGIN_RESPONSE"
    exit 1
fi

echo "✅ Login successful"
echo "Token: ${TOKEN:0:50}..."
echo ""

# Fetch photos
echo "3. Fetching photos..."
PHOTOS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/photos" \
    -H "Authorization: Bearer $TOKEN")

echo "Response:"
echo "$PHOTOS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$PHOTOS_RESPONSE"
echo ""

# Check for downloadUrl field
if echo "$PHOTOS_RESPONSE" | grep -q '"downloadUrl"'; then
    echo "✅ downloadUrl field is present in response"
    echo ""
    echo "Sample downloadUrl:"
    echo "$PHOTOS_RESPONSE" | python3 -c "import json,sys; photos=json.load(sys.stdin); print(photos[0].get('downloadUrl', 'null') if photos else 'No photos')" 2>/dev/null
else
    echo "❌ downloadUrl field is NOT present in response"
    echo ""
    echo "This means the backend is not generating presigned URLs."
    echo "The new deployment may not have fully rolled out yet."
fi
