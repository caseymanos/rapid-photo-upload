#!/bin/bash

# RapidPhotoUpload API Testing Script
# This script tests the deployed backend API endpoints

BASE_URL="http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com"
JWT_TOKEN=""

echo "================================================"
echo "RapidPhotoUpload API Testing Script"
echo "================================================"
echo ""
echo "Base URL: $BASE_URL"
echo ""

# Test 1: Health Check
echo "Test 1: Health Check"
echo "-------------------"
response=$(curl -s "$BASE_URL/actuator/health")
echo "Response: $response"
if echo "$response" | grep -q '"status":"UP"'; then
    echo "✅ PASSED: Health check is UP"
else
    echo "❌ FAILED: Health check is not UP"
fi
echo ""

# Test 2: User Registration
echo "Test 2: User Registration"
echo "------------------------"
timestamp=$(date +%s)
email="test_$timestamp@example.com"
password="TestPassword123!"

registration_response=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}")

echo "Response: $registration_response"
if echo "$registration_response" | grep -q "email"; then
    echo "✅ PASSED: User registration successful"
else
    echo "❌ FAILED: User registration failed"
fi
echo ""

# Test 3: User Login
echo "Test 3: User Login"
echo "-----------------"
login_response=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}")

echo "Response: $login_response"
JWT_TOKEN=$(echo "$login_response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -n "$JWT_TOKEN" ]; then
    echo "✅ PASSED: User login successful"
    echo "JWT Token: ${JWT_TOKEN:0:50}..."
else
    echo "❌ FAILED: User login failed - no token received"
fi
echo ""

# Test 4: Initiate Upload (requires JWT)
if [ -n "$JWT_TOKEN" ]; then
    echo "Test 4: Initiate Upload"
    echo "----------------------"
    initiate_response=$(curl -s -X POST "$BASE_URL/api/v1/uploads/initiate" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"originalFilename":"test-image.jpg","fileSizeBytes":1024000,"mimeType":"image/jpeg"}')

    echo "Response (truncated): ${initiate_response:0:200}..."
    PHOTO_ID=$(echo "$initiate_response" | grep -o '"photoId":"[^"]*' | cut -d'"' -f4)
    UPLOAD_ID=$(echo "$initiate_response" | grep -o '"multipartUploadId":"[^"]*' | cut -d'"' -f4)

    if [ -n "$PHOTO_ID" ] && [ -n "$UPLOAD_ID" ]; then
        echo "✅ PASSED: Upload session initiated"
        echo "Photo ID: $PHOTO_ID"
        echo "Multipart Upload ID: ${UPLOAD_ID:0:30}..."
    else
        echo "❌ FAILED: Upload initiation failed"
    fi
    echo ""

    # Note: The initiate endpoint returns presigned URLs directly,
    # so no additional API calls are needed for basic upload flow.
    # Tests 5-6 are skipped as they test old API patterns.
else
    echo "⚠️  SKIPPED: Tests 4-6 (no JWT token available)"
    echo ""
fi

# Test 7: Application Info
echo "Test 7: Application Info"
echo "-----------------------"
info_response=$(curl -s "$BASE_URL/actuator/info")
echo "Response: $info_response"
echo ""

# Summary
echo "================================================"
echo "Test Summary"
echo "================================================"
echo "Base URL: $BASE_URL"
echo "Test User: $email"
if [ -n "$JWT_TOKEN" ]; then
    echo "JWT Token: Available (${JWT_TOKEN:0:30}...)"
else
    echo "JWT Token: Not available"
fi
if [ -n "$SESSION_ID" ]; then
    echo "Session ID: $SESSION_ID"
fi
echo ""
echo "✅ All tests completed!"
echo ""
echo "Next steps:"
echo "1. Check uploaded files in S3: aws s3 ls s3://rapid-photo-upload-dev-photos-971422717446/ --recursive"
echo "2. Check CloudWatch logs: aws logs tail /ecs/rapid-photo-upload-dev --follow --since 5m"
echo "3. Test frontend: cd frontend-web && npm run dev"
echo "4. Test mobile app: cd mobile-app && npx expo start"
echo ""
