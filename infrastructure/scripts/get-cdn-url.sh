#!/bin/bash

# RapidPhotoUpload - Get CloudFront Distribution URL
# Quick helper script to retrieve the CDN URL for an environment

ENVIRONMENT=${1:-dev}

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
    echo "Error: Invalid environment. Use 'dev' or 'prod'"
    echo "Usage: $0 [dev|prod]"
    exit 1
fi

CDN_URL=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-frontend \
  --query 'Stacks[0].Outputs[?OutputKey==`WebsiteUrl`].OutputValue' \
  --output text 2>/dev/null)

if [ -z "$CDN_URL" ]; then
    echo "Error: Failed to retrieve CDN URL. Is the frontend stack deployed?"
    exit 1
fi

echo "$CDN_URL"

