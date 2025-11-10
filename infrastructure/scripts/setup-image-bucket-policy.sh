#!/bin/bash

# RapidPhotoUpload - Setup S3 Bucket Policy for CloudFront Image OAI
# This script manually adds the bucket policy to allow CloudFront to access photos

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${YELLOW}ℹ $1${NC}"; }

ENVIRONMENT=${1:-dev}

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
    print_error "Invalid environment. Use 'dev' or 'prod'"
    exit 1
fi

print_info "Setting up bucket policy for CloudFront Image OAI..."

# Get bucket name
BUCKET_NAME=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-storage \
  --query 'Stacks[0].Outputs[?OutputKey==`PhotoBucketName`].OutputValue' \
  --output text 2>/dev/null)

if [ -z "$BUCKET_NAME" ]; then
    print_error "Failed to retrieve bucket name"
    exit 1
fi

print_info "Bucket: $BUCKET_NAME"

# Get ImageOAI canonical user ID (look for ImageOAI logical resource)
OAI_ID=$(aws cloudformation describe-stack-resources \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-frontend \
  --query "StackResources[?contains(LogicalResourceId, 'ImageOAI') && ResourceType=='AWS::CloudFront::CloudFrontOriginAccessIdentity'].PhysicalResourceId" \
  --output text 2>/dev/null)

if [ -z "$OAI_ID" ]; then
    print_error "ImageOAI not found. Deploy frontend stack first."
    exit 1
fi

print_info "Origin Access Identity: $OAI_ID"

# Get OAI canonical user ID from CloudFormation (if available) or construct ARN
# CloudFront OAI ARN format: arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity <OAI_ID>
# For bucket policy, we use the ARN format directly
OAI_ARN="arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity $OAI_ID"

print_info "OAI ARN: $OAI_ARN"

# Get current bucket policy
CURRENT_POLICY=$(aws s3api get-bucket-policy \
  --bucket $BUCKET_NAME \
  --output text 2>/dev/null || echo '{}')

# Parse current policy and add CloudFront statement
if command -v jq &> /dev/null; then
    # Check if policy already has CloudFront statement
    HAS_CLOUDFRONT=$(echo "$CURRENT_POLICY" | jq -r ".Statement[]? | select(.Principal.AWS | contains(\"$OAI_ID\"))" 2>/dev/null || echo "")
    
    if [ -n "$HAS_CLOUDFRONT" ]; then
        print_info "Bucket policy already includes CloudFront access"
        exit 0
    fi
    
    # Create new policy with CloudFront access
    NEW_POLICY=$(echo "$CURRENT_POLICY" | jq --arg oai_arn "$OAI_ARN" --arg bucket "$BUCKET_NAME" '
      .Statement += [{
        "Sid": "AllowCloudFrontImageAccess",
        "Effect": "Allow",
        "Principal": {
          "AWS": $oai_arn
        },
        "Action": "s3:GetObject",
        "Resource": "arn:aws:s3:::" + $bucket + "/*"
      }]
    ')
    
    # Apply new policy
    echo "$NEW_POLICY" | aws s3api put-bucket-policy \
      --bucket $BUCKET_NAME \
      --policy file:///dev/stdin
    
    print_success "Bucket policy updated successfully"
else
    print_error "jq is required for this script"
    print_info "Install: brew install jq"
    exit 1
fi

