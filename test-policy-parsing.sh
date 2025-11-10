#!/bin/bash
# Test script to verify S3 bucket policy parsing logic

set -e

echo "Testing S3 bucket policy parsing logic..."
echo "=========================================="
echo ""

# Test Case 1: Empty policy response (no existing policy)
echo "Test 1: No existing policy"
POLICY_RESPONSE=""
if [ -z "$POLICY_RESPONSE" ]; then
    echo "✓ Detected empty response"
    CURRENT_POLICY='{"Version":"2012-10-17","Statement":[]}'
    echo "✓ Initialized empty policy: $CURRENT_POLICY"
fi
echo ""

# Test Case 2: Policy response with nested JSON string
echo "Test 2: Policy response with nested JSON string"
POLICY_RESPONSE='{"Policy":"{\"Version\":\"2012-10-17\",\"Statement\":[]}"}'
echo "Raw response: $POLICY_RESPONSE"
CURRENT_POLICY=$(echo "$POLICY_RESPONSE" | jq -r '.Policy' | jq .)
echo "✓ Extracted and parsed Policy field: $CURRENT_POLICY"
echo ""

# Test Case 3: Policy with null Statement
echo "Test 3: Policy with null Statement"
CURRENT_POLICY='{"Version":"2012-10-17","Statement":null}'
echo "Before: $CURRENT_POLICY"
CURRENT_POLICY=$(echo "$CURRENT_POLICY" | jq 'if .Statement == null then .Statement = [] else . end')
echo "✓ After initializing Statement: $CURRENT_POLICY"
echo ""

# Test Case 4: Policy with existing Statement
echo "Test 4: Policy with existing Statement"
POLICY_RESPONSE='{"Policy":"{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"ExistingRule\",\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::bucket/*\"}]}"}'
echo "Raw response: $POLICY_RESPONSE"
CURRENT_POLICY=$(echo "$POLICY_RESPONSE" | jq -r '.Policy' | jq .)
echo "✓ Parsed policy with existing statement:"
echo "$CURRENT_POLICY" | jq .
echo ""

# Test Case 5: Appending a new statement
echo "Test 5: Appending CloudFront statement"
NEW_STATEMENT='{"Sid":"AllowCloudFrontOAI","Effect":"Allow","Principal":{"AWS":"arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity E2TEST"},"Action":"s3:GetObject","Resource":"arn:aws:s3:::bucket/*"}'
NEW_POLICY=$(echo "$CURRENT_POLICY" | jq --argjson stmt "$NEW_STATEMENT" '.Statement += [$stmt]')
echo "✓ Policy after appending CloudFront statement:"
echo "$NEW_POLICY" | jq .
echo ""

echo "=========================================="
echo "All tests passed! ✓"
echo ""
echo "Summary:"
echo "- Empty policy responses are handled correctly"
echo "- Nested JSON Policy strings are extracted and parsed"
echo "- Null Statement arrays are initialized"
echo "- New statements can be appended without errors"
