#!/bin/bash

# RapidPhotoUpload - Update ECS Task Definition with CDN Configuration
# This script updates the ECS task definition to include CDN environment variables

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

# Get arguments
ENVIRONMENT=${1:-dev}
CDN_URL=${2}
ENABLE_CDN=${3:-false}
ENABLE_FEATURE_FLAG=${4:-false}

if [ -z "$CDN_URL" ]; then
    print_error "CDN URL is required"
    echo "Usage: $0 <environment> <cdn-url> [enable-cdn] [enable-feature-flag]"
    echo "Example: $0 dev https://d123.cloudfront.net true false"
    exit 1
fi

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
    print_error "Invalid environment. Use 'dev' or 'prod'"
    exit 1
fi

export CDK_DEFAULT_REGION=${AWS_REGION:-us-east-1}

print_info "Updating ECS task definition for environment: $ENVIRONMENT"
print_info "CDN URL: $CDN_URL"
print_info "CDN Enabled: $ENABLE_CDN"
print_info "Feature Flag Enabled: $ENABLE_FEATURE_FLAG"

# Get cluster and service names
CLUSTER_NAME=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`ClusterName`].OutputValue' \
  --output text 2>/dev/null)

SERVICE_NAME=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`ServiceName`].OutputValue' \
  --output text 2>/dev/null)

if [ -z "$CLUSTER_NAME" ] || [ -z "$SERVICE_NAME" ]; then
    print_error "Failed to retrieve cluster or service name"
    exit 1
fi

print_step "Retrieving current task definition..."
TASK_DEF_ARN=$(aws ecs describe-services \
  --cluster $CLUSTER_NAME \
  --services $SERVICE_NAME \
  --query 'services[0].taskDefinition' \
  --output text)

if [ -z "$TASK_DEF_ARN" ]; then
    print_error "Failed to retrieve task definition"
    exit 1
fi

print_step "Fetching task definition..."
TASK_DEF=$(aws ecs describe-task-definition \
  --task-definition $TASK_DEF_ARN \
  --query 'taskDefinition')

# Create temporary file for updated task definition
TEMP_FILE=$(mktemp)
echo "$TASK_DEF" > $TEMP_FILE

print_step "Updating task definition with CDN configuration..."

# Check for jq dependency
if ! command -v jq &> /dev/null; then
    print_error "jq is required for this script."
    print_info "Install jq:"
    echo "  macOS: brew install jq"
    echo "  Linux: sudo apt-get install jq  # or yum install jq"
    echo "  Or download from: https://stedolan.github.io/jq/download/"
    exit 1
fi

# Use jq to update environment variables
if command -v jq &> /dev/null; then
    UPDATED_TASK_DEF=$(jq --arg cdn_url "$CDN_URL" \
      --arg cdn_enabled "$ENABLE_CDN" \
      --arg feature_flag "$ENABLE_FEATURE_FLAG" \
      '.containerDefinitions[0].environment = (.containerDefinitions[0].environment // []) | 
       .containerDefinitions[0].environment |= map(select(.name != "CDN_BASE_URL" and .name != "CDN_IMAGES_ENABLED" and .name != "FEATURE_FLAG_IMAGE_CDN")) |
       .containerDefinitions[0].environment += [
         {"name": "CDN_BASE_URL", "value": $cdn_url},
         {"name": "CDN_IMAGES_ENABLED", "value": $cdn_enabled},
         {"name": "FEATURE_FLAG_IMAGE_CDN", "value": $feature_flag}
       ] |
       del(.taskDefinitionArn) | del(.revision) | del(.status) | del(.requiresAttributes) | del(.compatibilities) | del(.registeredAt) | del(.registeredBy)' \
      $TEMP_FILE)
    
    echo "$UPDATED_TASK_DEF" > $TEMP_FILE
    print_success "Task definition updated"
else
    print_error "jq is required for this script. Please install jq: brew install jq"
    rm $TEMP_FILE
    exit 1
fi

print_step "Registering new task definition..."
NEW_TASK_DEF_ARN=$(aws ecs register-task-definition \
  --cli-input-json file://$TEMP_FILE \
  --query 'taskDefinition.taskDefinitionArn' \
  --output text)

if [ -z "$NEW_TASK_DEF_ARN" ]; then
    print_error "Failed to register new task definition"
    rm $TEMP_FILE
    exit 1
fi

print_success "New task definition registered: $NEW_TASK_DEF_ARN"

# Clean up temp file
rm $TEMP_FILE

print_step "Updating ECS service..."
aws ecs update-service \
  --cluster $CLUSTER_NAME \
  --service $SERVICE_NAME \
  --task-definition $NEW_TASK_DEF_ARN \
  --force-new-deployment \
  --no-cli-pager > /dev/null

print_success "ECS service update initiated"

print_step "Waiting for service to stabilize (this may take 2-5 minutes)..."
aws ecs wait services-stable \
  --cluster $CLUSTER_NAME \
  --services $SERVICE_NAME

print_success "Service updated successfully!"

echo ""
echo "========================================"
echo "CDN Configuration Update Summary"
echo "========================================"
echo "Environment:              $ENVIRONMENT"
echo "CDN URL:                   $CDN_URL"
echo "CDN Enabled:               $ENABLE_CDN"
echo "Feature Flag Enabled:      $ENABLE_FEATURE_FLAG"
echo "New Task Definition:       $NEW_TASK_DEF_ARN"
echo ""
echo "Next Steps:"
if [ "$ENABLE_FEATURE_FLAG" = "false" ]; then
    echo "1. Test CDN endpoints manually"
    echo "2. Verify backend logs show CDN URL generation"
    echo "3. Enable feature flag: $0 $ENVIRONMENT $CDN_URL $ENABLE_CDN true"
else
    echo "1. Monitor CloudFront metrics"
    echo "2. Verify frontend/mobile apps are using CDN URLs"
    echo "3. Check performance improvements"
fi
echo ""

