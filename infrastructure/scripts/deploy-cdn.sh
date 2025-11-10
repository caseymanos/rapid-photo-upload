#!/bin/bash

# RapidPhotoUpload - CDN Image Rollout Deployment Script
# This script deploys storage and frontend stacks for CDN support, then updates compute stack with CDN URL

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
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

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."

    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI not found. Please install AWS CLI v2."
        exit 1
    fi
    print_success "AWS CLI found: $(aws --version)"

    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js not found. Please install Node.js 18.x or later."
        exit 1
    fi
    print_success "Node.js found: $(node --version)"

    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS credentials not configured. Run 'aws configure'"
        exit 1
    fi
    print_success "AWS credentials configured"
}

# Get environment argument
ENVIRONMENT=${1:-dev}

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
    print_error "Invalid environment. Use 'dev' or 'prod'"
    echo "Usage: $0 [dev|prod]"
    exit 1
fi

print_info "Deploying CDN infrastructure for environment: $ENVIRONMENT"

# Set AWS environment variables
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=${AWS_REGION:-us-east-1}

print_info "AWS Account: $CDK_DEFAULT_ACCOUNT"
print_info "AWS Region: $CDK_DEFAULT_REGION"

# Check prerequisites
check_prerequisites

# Navigate to infrastructure directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
INFRASTRUCTURE_DIR="$(dirname "$SCRIPT_DIR")"

cd "$INFRASTRUCTURE_DIR"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    print_info "Installing CDK dependencies..."
    npm install
    print_success "Dependencies installed"
fi

# Build TypeScript
print_info "Building CDK app..."
npm run build
print_success "CDK app built"

# Step 1: Deploy Storage Stack
print_step "Step 1: Deploying Storage Stack..."
npx cdk deploy rapid-photo-upload-${ENVIRONMENT}-storage --context environment=$ENVIRONMENT --require-approval never

if [ $? -ne 0 ]; then
    print_error "Storage stack deployment failed"
    exit 1
fi
print_success "Storage stack deployed successfully"

# Step 2: Deploy Frontend Stack (includes CloudFront distribution)
print_step "Step 2: Deploying Frontend Stack (CloudFront + S3)..."
npx cdk deploy rapid-photo-upload-${ENVIRONMENT}-frontend --context environment=$ENVIRONMENT --require-approval never

if [ $? -ne 0 ]; then
    print_error "Frontend stack deployment failed"
    exit 1
fi
print_success "Frontend stack deployed successfully"

# Step 3: Get CloudFront Distribution URL
print_step "Step 3: Retrieving CloudFront Distribution URL..."
DISTRIBUTION_URL=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-frontend \
  --query 'Stacks[0].Outputs[?OutputKey==`WebsiteUrl`].OutputValue' \
  --output text 2>/dev/null)

if [ -z "$DISTRIBUTION_URL" ]; then
    print_error "Failed to retrieve CloudFront distribution URL"
    print_info "You can manually retrieve it with:"
    echo "  aws cloudformation describe-stacks --stack-name rapid-photo-upload-${ENVIRONMENT}-frontend --query 'Stacks[0].Outputs[?OutputKey==\`WebsiteUrl\`].OutputValue' --output text"
    exit 1
fi

print_success "CloudFront Distribution URL: $DISTRIBUTION_URL"

# Step 4: Update Compute Stack with CDN URL (if compute stack exists)
print_step "Step 4: Checking if Compute Stack needs CDN URL update..."
COMPUTE_STACK_EXISTS=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-compute \
  --query 'Stacks[0].StackName' \
  --output text 2>/dev/null || echo "")

if [ -n "$COMPUTE_STACK_EXISTS" ]; then
    print_info "Compute stack exists. CDN URL will be set via ECS task definition update."
    print_info "To enable CDN in the backend, update the ECS task definition with:"
    echo ""
    echo "  CDN_BASE_URL=$DISTRIBUTION_URL"
    echo "  CDN_IMAGES_ENABLED=true"
    echo "  FEATURE_FLAG_IMAGE_CDN=false  # Set to true after testing"
    echo ""
    print_info "You can update the task definition using:"
    echo "  ./scripts/update-cdn-config.sh $ENVIRONMENT $DISTRIBUTION_URL"
else
    print_info "Compute stack does not exist yet. CDN URL will be set when compute stack is deployed."
fi

# Print summary
echo ""
echo "========================================"
echo "CDN Deployment Summary"
echo "========================================"
echo "Environment:              $ENVIRONMENT"
echo "CloudFront Distribution:   $DISTRIBUTION_URL"
echo "Image CDN Path:           $DISTRIBUTION_URL/images/*"
echo ""
echo "Next Steps:"
echo "1. Test CDN endpoint: curl \"$DISTRIBUTION_URL/images/<s3-key>?w=480&fmt=webp\""
echo "2. Update backend ECS task definition with CDN environment variables"
echo "3. Enable feature flag after testing: FEATURE_FLAG_IMAGE_CDN=true"
echo "4. Follow CDN_IMAGE_ROLLOUT.md for staged activation"
echo ""
print_success "CDN infrastructure deployment complete!"

