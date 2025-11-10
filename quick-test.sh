#!/bin/bash

# Quick test to verify optimizations are working
# This simulates an upload to trigger performance logging

echo "Quick Performance Test"
echo "======================"
echo ""

# Test initiate upload endpoint (should trigger parallel URL generation)
echo "Testing upload initiation (triggers parallel URL generation)..."
echo ""

# Create a test upload request
curl -X POST http://localhost:8080/api/v1/uploads/initiate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "originalFilename": "test-photo.jpg",
    "fileSizeBytes": 10485760,
    "mimeType": "image/jpeg"
  }' 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "Auth required - this is expected"

echo ""
echo "Check backend logs for:"
echo "  'Generated X presigned URLs in Xms (parallel)'"
echo ""
echo "Run: tail -f backend/backend.out | grep parallel"
echo ""
echo "Or open browser to http://localhost:3004 and upload real photos!"
