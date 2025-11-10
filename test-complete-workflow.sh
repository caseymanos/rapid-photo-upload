#!/bin/bash

# RapidPhotoUpload - Complete Workflow Test
# Tests the entire upload flow: register → login → initiate → upload → complete → retrieve

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://rapid-photo-upload-dev-alb-1693661982.us-east-1.elb.amazonaws.com/api/v1"
TEST_EMAIL="workflow-test-$(date +%s)@example.com"
TEST_PASSWORD="TestPass123!"

echo -e "${YELLOW}Testing Complete Upload Workflow${NC}"
echo "=================================="
echo "Backend: $BASE_URL"
echo "Test User: $TEST_EMAIL"
echo ""

# Step 1: Register
echo -e "${YELLOW}Step 1: Registering user...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

echo "$REGISTER_RESPONSE" | jq .

TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.token')
USER_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.userId')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo -e "${RED}✗ Registration failed${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Registration successful${NC}"
echo "User ID: $USER_ID"
echo ""

# Step 2: Login (verify credentials work)
echo -e "${YELLOW}Step 2: Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

echo "$LOGIN_RESPONSE" | jq .

LOGIN_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')
if [ "$LOGIN_TOKEN" == "null" ] || [ -z "$LOGIN_TOKEN" ]; then
  echo -e "${RED}✗ Login failed${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Login successful${NC}"
echo ""

# Step 3: Initiate upload
echo -e "${YELLOW}Step 3: Initiating upload...${NC}"
INITIATE_RESPONSE=$(curl -s -X POST "$BASE_URL/uploads/initiate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "originalFilename": "test-workflow.jpg",
    "fileSizeBytes": 1048576,
    "mimeType": "image/jpeg"
  }')

echo "$INITIATE_RESPONSE" | jq .

PHOTO_ID=$(echo "$INITIATE_RESPONSE" | jq -r '.photoId')
PRESIGNED_URL=$(echo "$INITIATE_RESPONSE" | jq -r '.presignedUrls[0].url')

if [ "$PHOTO_ID" == "null" ] || [ -z "$PHOTO_ID" ]; then
  echo -e "${RED}✗ Upload initiation failed${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Upload initiated${NC}"
echo "Photo ID: $PHOTO_ID"
echo ""

# Step 4: Upload to S3 (create dummy file)
echo -e "${YELLOW}Step 4: Uploading to S3...${NC}"
TEMP_FILE=$(mktemp)
dd if=/dev/urandom of="$TEMP_FILE" bs=1024 count=1024 2>/dev/null  # 1MB file

UPLOAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$PRESIGNED_URL" \
  -H "Content-Type: image/jpeg" \
  --data-binary "@$TEMP_FILE")

HTTP_CODE=$(echo "$UPLOAD_RESPONSE" | tail -n1)
ETAG=$(echo "$UPLOAD_RESPONSE" | grep -i "etag" || echo "")

if [ "$HTTP_CODE" != "200" ]; then
  echo -e "${RED}✗ S3 upload failed (HTTP $HTTP_CODE)${NC}"
  rm -f "$TEMP_FILE"
  exit 1
fi

# Extract ETag from response headers
ETAG=$(curl -s -I -X PUT "$PRESIGNED_URL" \
  -H "Content-Type: image/jpeg" \
  --data-binary "@$TEMP_FILE" 2>&1 | grep -i "etag:" | cut -d' ' -f2 | tr -d '\r\n"')

echo -e "${GREEN}✓ S3 upload successful${NC}"
echo "ETag: $ETAG"
rm -f "$TEMP_FILE"
echo ""

# Step 5: Complete upload
echo -e "${YELLOW}Step 5: Completing upload...${NC}"
COMPLETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/uploads/$PHOTO_ID/complete" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"parts\": [{
      \"partNumber\": 1,
      \"etag\": \"$ETAG\",
      \"sizeBytes\": 1048576
    }]
  }")

COMPLETE_HTTP_CODE=$(echo "$COMPLETE_RESPONSE" | tail -n1)

if [ "$COMPLETE_HTTP_CODE" == "200" ] || [ "$COMPLETE_HTTP_CODE" == "204" ]; then
  echo -e "${GREEN}✓ Upload completion successful (HTTP $COMPLETE_HTTP_CODE)${NC}"
else
  echo -e "${RED}✗ Upload completion failed (HTTP $COMPLETE_HTTP_CODE)${NC}"
  echo "$COMPLETE_RESPONSE"
  exit 1
fi
echo ""

# Step 6: Retrieve photos
echo -e "${YELLOW}Step 6: Retrieving photos...${NC}"
PHOTOS_RESPONSE=$(curl -s -X GET "$BASE_URL/photos" \
  -H "Authorization: Bearer $TOKEN")

echo "$PHOTOS_RESPONSE" | jq .

PHOTO_COUNT=$(echo "$PHOTOS_RESPONSE" | jq '. | length')
if [ "$PHOTO_COUNT" -ge 1 ]; then
  echo -e "${GREEN}✓ Photos retrieved successfully ($PHOTO_COUNT photos)${NC}"
else
  echo -e "${RED}✗ No photos found${NC}"
  exit 1
fi
echo ""

# Summary
echo "=================================="
echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
echo ""
echo "Summary:"
echo "- Registration: ✓"
echo "- Login: ✓"
echo "- Upload Initiation: ✓"
echo "- S3 Upload: ✓"
echo "- Upload Completion: ✓"
echo "- Photo Retrieval: ✓"
echo ""
echo "Test User: $TEST_EMAIL"
echo "Photo ID: $PHOTO_ID"
echo "JWT Token: ${TOKEN:0:20}..."
echo ""
echo -e "${GREEN}Backend is ready for frontend/mobile testing!${NC}"
