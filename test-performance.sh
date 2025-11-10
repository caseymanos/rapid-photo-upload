#!/bin/bash

# Performance Testing Script for RapidPhotoUpload
# Tests the optimizations applied to the system

echo "======================================"
echo "RapidPhotoUpload Performance Tests"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if backend is running
echo "1. Checking backend status..."
BACKEND_HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null)
if echo "$BACKEND_HEALTH" | grep -q "UP"; then
    echo -e "${GREEN}✓ Backend is running${NC}"
else
    echo -e "${RED}✗ Backend is not running${NC}"
    echo "  Please start backend: cd backend && mvn spring-boot:run"
    exit 1
fi

# Check if frontend is running (try common ports)
echo "2. Checking frontend status..."
FRONTEND_PORT=""
for port in 3004 5173 3000; do
    FRONTEND_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port 2>/dev/null)
    if [ "$FRONTEND_CHECK" = "200" ]; then
        FRONTEND_PORT=$port
        break
    fi
done

if [ -n "$FRONTEND_PORT" ]; then
    echo -e "${GREEN}✓ Frontend is running on port $FRONTEND_PORT${NC}"
else
    echo -e "${RED}✗ Frontend is not running${NC}"
    echo "  Please start frontend: cd frontend-web && npm run dev"
    exit 1
fi

echo ""
echo "======================================"
echo "Code Verification"
echo "======================================"

# Check for parallel optimization in S3StorageService
echo "3. Verifying parallel URL generation..."
if grep -q "parallel()" backend/src/main/java/com/rapidphotoupload/infrastructure/storage/S3StorageService.java; then
    echo -e "${GREEN}✓ Parallel URL generation implemented${NC}"
else
    echo -e "${RED}✗ Parallel URL generation NOT found${NC}"
fi

# Check for parallelStream in GetPhotosQueryHandler
echo "4. Verifying parallel photo queries..."
if grep -q "parallelStream" backend/src/main/java/com/rapidphotoupload/application/handler/GetPhotosQueryHandler.java; then
    echo -e "${GREEN}✓ Parallel photo queries implemented${NC}"
else
    echo -e "${RED}✗ Parallel photo queries NOT found${NC}"
fi

# Check for performance logging in frontend
echo "5. Verifying frontend performance instrumentation..."
if grep -q "Performance" frontend-web/src/features/upload/services/uploadService.ts; then
    echo -e "${GREEN}✓ Frontend performance logging implemented${NC}"
else
    echo -e "${RED}✗ Frontend performance logging NOT found${NC}"
fi

# Check for gallery preloading
echo "6. Verifying gallery preloading..."
if grep -q "preloadGallery" frontend-web/src/features/upload/pages/UploadPage.tsx; then
    echo -e "${GREEN}✓ Gallery preloading implemented${NC}"
else
    echo -e "${RED}✗ Gallery preloading NOT found${NC}"
fi

# Check API timeout optimization
echo "7. Verifying API timeout optimization..."
if grep -q "timeout: 15000" frontend-web/src/shared/api/apiClient.ts; then
    echo -e "${GREEN}✓ API timeout reduced to 15s${NC}"
else
    echo -e "${YELLOW}⚠ API timeout may not be optimized${NC}"
fi

echo ""
echo "======================================"
echo "API Endpoint Tests"
echo "======================================"

# Test health endpoint
echo "8. Testing health endpoint..."
START=$(date +%s%N)
HEALTH_RESPONSE=$(curl -s http://localhost:8080/actuator/health)
END=$(date +%s%N)
DURATION=$((($END - $START) / 1000000))
if echo "$HEALTH_RESPONSE" | grep -q "UP"; then
    echo -e "${GREEN}✓ Health endpoint responding (${DURATION}ms)${NC}"
else
    echo -e "${RED}✗ Health endpoint error${NC}"
fi

# Test metrics endpoint (should include performance metrics)
echo "9. Testing metrics endpoint..."
METRICS_RESPONSE=$(curl -s http://localhost:8080/actuator/metrics 2>/dev/null)
if echo "$METRICS_RESPONSE" | grep -q "names"; then
    echo -e "${GREEN}✓ Metrics endpoint responding${NC}"
else
    echo -e "${YELLOW}⚠ Metrics endpoint may not be available${NC}"
fi

echo ""
echo "======================================"
echo "Performance Recommendations"
echo "======================================"
echo ""
echo "To test the full performance improvements:"
echo ""
echo "1. Open browser to: http://localhost:\${FRONTEND_PORT:-3004}"
echo "2. Open DevTools Console (F12)"
echo "3. Navigate to Upload page"
echo "4. Upload 10+ images (5-15MB each)"
echo ""
echo "Expected console output:"
echo "  [Performance] Upload complete for photo.jpg (12.5MB):"
echo "    Total: 3200ms"
echo "    Initiate: 250ms"
echo "    Upload: 2750ms (36.4 Mbps)"
echo "    Complete: 200ms"
echo ""
echo "  [Performance] Gallery preloaded in 1834.23ms"
echo ""
echo "5. Click 'View Gallery' - should load instantly!"
echo ""
echo "Backend logs should show:"
echo "  INFO - Generated 20 presigned URLs in 387ms (parallel)"
echo "  INFO - Retrieved 100 photos in 2134ms (parallel presigned URL generation)"
echo ""
echo "======================================"
echo "For detailed testing instructions, see:"
echo "  PERFORMANCE_TESTING_GUIDE.md"
echo "======================================"
