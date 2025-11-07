#!/bin/bash

# RapidPhotoUpload Backend - API Test Script
# Tests the main endpoints to verify the application is working

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🧪 Testing RapidPhotoUpload Backend API"
echo "========================================"
echo ""

# Test 1: Health Check
echo -n "1. Health Check: "
HEALTH=$(curl -s -w "%{http_code}" -o /tmp/health.json ${BASE_URL}/actuator/health)
if [ "$HEALTH" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP 200)"
    cat /tmp/health.json | jq '.' 2>/dev/null || cat /tmp/health.json
else
    echo -e "${RED}✗ FAIL${NC} (HTTP $HEALTH)"
fi
echo ""

# Test 2: Register a new user
echo -n "2. User Registration: "
REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d '{
        "email": "testuser@example.com",
        "password": "SecurePassword123!",
        "username": "testuser"
    }')

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n1)
BODY=$(echo "$REGISTER_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP $HTTP_CODE)"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    JWT_TOKEN=$(echo "$BODY" | jq -r '.token' 2>/dev/null)
elif [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "409" ]; then
    echo -e "${YELLOW}⚠ SKIP${NC} (User already exists - HTTP $HTTP_CODE)"
    # Try login instead
    LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{
            "email": "testuser@example.com",
            "password": "SecurePassword123!"
        }')
    LOGIN_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
    LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | head -n-1)
    if [ "$LOGIN_CODE" = "200" ]; then
        JWT_TOKEN=$(echo "$LOGIN_BODY" | jq -r '.token' 2>/dev/null)
        echo "  Logged in successfully"
    fi
else
    echo -e "${RED}✗ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$BODY"
fi
echo ""

# Test 3: Login
echo -n "3. User Login: "
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
        "email": "testuser@example.com",
        "password": "SecurePassword123!"
    }')

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
BODY=$(echo "$LOGIN_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP $HTTP_CODE)"
    JWT_TOKEN=$(echo "$BODY" | jq -r '.token' 2>/dev/null)
    echo "  Token: ${JWT_TOKEN:0:20}..."
else
    echo -e "${RED}✗ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$BODY"
fi
echo ""

# Test 4: Get Photos (requires authentication)
if [ ! -z "$JWT_TOKEN" ]; then
    echo -n "4. Get Photos (Authenticated): "
    PHOTOS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET ${BASE_URL}/api/v1/photos \
        -H "Authorization: Bearer $JWT_TOKEN")

    HTTP_CODE=$(echo "$PHOTOS_RESPONSE" | tail -n1)
    BODY=$(echo "$PHOTOS_RESPONSE" | head -n-1)

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $HTTP_CODE)"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $HTTP_CODE)"
        echo "$BODY"
    fi
else
    echo -e "${YELLOW}4. Get Photos: SKIPPED (no JWT token)${NC}"
fi
echo ""

# Test 5: Metrics endpoint
echo -n "5. Metrics Endpoint: "
METRICS=$(curl -s -w "%{http_code}" -o /tmp/metrics.txt ${BASE_URL}/actuator/metrics)
if [ "$METRICS" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP 200)"
    head -5 /tmp/metrics.txt
else
    echo -e "${RED}✗ FAIL${NC} (HTTP $METRICS)"
fi
echo ""

echo "========================================"
echo "✅ Test suite complete!"
echo ""
echo "Next steps:"
echo "  - Review SETUP_COMPLETE.md for full API documentation"
echo "  - Test upload flow: POST /api/v1/uploads/initiate"
echo "  - Monitor metrics: GET /actuator/metrics"
