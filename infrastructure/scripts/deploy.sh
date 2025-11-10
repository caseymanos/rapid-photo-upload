#!/bin/bash

# RapidPhotoUpload - Deployment Helper Script
# This script automates the deployment process

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker not found. Please install Docker."
        exit 1
    fi
    print_success "Docker found: $(docker --version)"

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

print_info "Deploying to environment: $ENVIRONMENT"

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
BACKEND_DIR="$(dirname "$INFRASTRUCTURE_DIR")/backend"

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

# Deploy infrastructure
print_info "Deploying infrastructure stacks..."
npx cdk deploy --all --context environment=$ENVIRONMENT --require-approval never

if [ $? -ne 0 ]; then
    print_error "Infrastructure deployment failed"
    exit 1
fi

print_success "Infrastructure deployed successfully"

# Get stack outputs
print_info "Retrieving stack outputs..."

ECR_URI=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`RepositoryUri`].OutputValue' \
  --output text)

ALB_DNS=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDnsName`].OutputValue' \
  --output text)

CLUSTER_NAME=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`ClusterName`].OutputValue' \
  --output text)

SERVICE_NAME=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-${ENVIRONMENT}-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`ServiceName`].OutputValue' \
  --output text)

print_success "Stack outputs retrieved"
print_info "ECR Repository: $ECR_URI"
print_info "Application URL: http://$ALB_DNS"

# Ask if user wants to build and deploy application
read -p "Do you want to build and deploy the application? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Build backend
    print_info "Building backend application..."
    cd "$BACKEND_DIR"

    if [ -f "mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi

    if [ $? -ne 0 ]; then
        print_error "Backend build failed"
        exit 1
    fi

    print_success "Backend built successfully"

    # Build Docker image
    print_info "Building Docker image..."
    docker build -t rapid-photo-upload:latest .

    if [ $? -ne 0 ]; then
        print_error "Docker build failed"
        exit 1
    fi

    print_success "Docker image built"

    # Push to ECR
    print_info "Authenticating with ECR..."
    aws ecr get-login-password --region $CDK_DEFAULT_REGION | \
      docker login --username AWS --password-stdin $ECR_URI

    print_info "Tagging and pushing image to ECR..."
    docker tag rapid-photo-upload:latest $ECR_URI:latest
    docker push $ECR_URI:latest

    if [ $? -ne 0 ]; then
        print_error "Failed to push image to ECR"
        exit 1
    fi

    print_success "Image pushed to ECR"

    # Update ECS service
    print_info "Updating ECS service..."
    aws ecs update-service \
      --cluster $CLUSTER_NAME \
      --service $SERVICE_NAME \
      --force-new-deployment \
      --region $CDK_DEFAULT_REGION \
      --no-cli-pager

    print_success "ECS service updated"

    # Wait for deployment
    print_info "Waiting for deployment to complete (this may take 2-5 minutes)..."
    aws ecs wait services-stable \
      --cluster $CLUSTER_NAME \
      --services $SERVICE_NAME \
      --region $CDK_DEFAULT_REGION

    print_success "Deployment completed successfully!"

    # Test health endpoint
    print_info "Testing health endpoint..."
    sleep 10  # Give ALB a moment to register targets

    HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://$ALB_DNS/actuator/health || echo "000")

    if [ "$HEALTH_STATUS" = "200" ]; then
        print_success "Health check passed!"
    else
        print_error "Health check failed (HTTP $HEALTH_STATUS)"
        print_info "The application may still be starting. Check logs with:"
        echo "  aws logs tail /ecs/rapid-photo-upload-${ENVIRONMENT} --follow"
    fi
fi

# Print summary
echo ""
echo "========================================"
echo "Deployment Summary"
echo "========================================"
echo "Environment:       $ENVIRONMENT"
echo "Application URL:   http://$ALB_DNS"
echo "ECS Cluster:       $CLUSTER_NAME"
echo "ECS Service:       $SERVICE_NAME"
echo ""
echo "Next steps:"
echo "1. Test the application: curl http://$ALB_DNS/actuator/health"
echo "2. View logs: aws logs tail /ecs/rapid-photo-upload-${ENVIRONMENT} --follow"
echo "3. View dashboard: https://console.aws.amazon.com/cloudwatch/home?region=${CDK_DEFAULT_REGION}#dashboards:name=rapid-photo-upload-${ENVIRONMENT}"
echo ""
print_success "All done!"
