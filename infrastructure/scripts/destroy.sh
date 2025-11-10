#!/bin/bash

# RapidPhotoUpload - Infrastructure Destruction Script
# WARNING: This will delete all resources and data!

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_warning() {
    echo -e "${RED}⚠ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Get environment argument
ENVIRONMENT=${1:-dev}

if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
    print_warning "Invalid environment. Use 'dev' or 'prod'"
    echo "Usage: $0 [dev|prod]"
    exit 1
fi

# Warning message
echo ""
print_warning "═══════════════════════════════════════════════════════════"
print_warning "  WARNING: DESTRUCTIVE OPERATION"
print_warning "═══════════════════════════════════════════════════════════"
echo ""
echo "This will DELETE the following resources in environment: $ENVIRONMENT"
echo ""
echo "  • VPC and all networking resources"
echo "  • RDS PostgreSQL database and ALL DATA"
echo "  • S3 bucket and ALL PHOTOS"
echo "  • ECS cluster and all running tasks"
echo "  • ECR repository and all Docker images"
echo "  • Application Load Balancer"
echo "  • CloudWatch logs and dashboards"
echo "  • All IAM roles and policies"
echo "  • Secrets in AWS Secrets Manager"
echo ""
print_warning "THIS OPERATION CANNOT BE UNDONE!"
echo ""

# Confirmation
read -p "Are you absolutely sure you want to destroy the $ENVIRONMENT environment? (yes/no): " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    print_info "Destruction cancelled"
    exit 0
fi

# Double confirmation for production
if [[ "$ENVIRONMENT" == "prod" ]]; then
    print_warning "You are about to destroy PRODUCTION!"
    read -p "Type 'DELETE PRODUCTION' to confirm: " -r
    echo
    if [[ $REPLY != "DELETE PRODUCTION" ]]; then
        print_info "Destruction cancelled"
        exit 0
    fi
fi

# Set AWS environment variables
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=${AWS_REGION:-us-east-1}

# Navigate to infrastructure directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
INFRASTRUCTURE_DIR="$(dirname "$SCRIPT_DIR")"
cd "$INFRASTRUCTURE_DIR"

# Empty S3 bucket first (CDK can't delete non-empty buckets in some cases)
print_info "Emptying S3 bucket..."
BUCKET_NAME="rapid-photo-upload-${ENVIRONMENT}"

if aws s3 ls "s3://$BUCKET_NAME" 2>/dev/null; then
    aws s3 rm "s3://$BUCKET_NAME" --recursive
    print_success "S3 bucket emptied"
else
    print_info "S3 bucket not found or already empty"
fi

# Empty ECR repository
print_info "Removing Docker images from ECR..."
REPO_NAME="rapid-photo-upload-${ENVIRONMENT}"

if aws ecr describe-repositories --repository-names "$REPO_NAME" &>/dev/null; then
    IMAGE_IDS=$(aws ecr list-images --repository-name "$REPO_NAME" \
      --query 'imageIds[*]' --output json)

    if [ "$IMAGE_IDS" != "[]" ]; then
        aws ecr batch-delete-image \
          --repository-name "$REPO_NAME" \
          --image-ids "$IMAGE_IDS"
        print_success "ECR images deleted"
    else
        print_info "ECR repository already empty"
    fi
else
    print_info "ECR repository not found"
fi

# Stop all ECS tasks
print_info "Stopping ECS tasks..."
CLUSTER_NAME="rapid-photo-upload-${ENVIRONMENT}-cluster"

if aws ecs describe-clusters --clusters "$CLUSTER_NAME" --query 'clusters[0].status' --output text 2>/dev/null | grep -q ACTIVE; then
    TASK_ARNS=$(aws ecs list-tasks --cluster "$CLUSTER_NAME" --query 'taskArns' --output text)

    if [ -n "$TASK_ARNS" ]; then
        for TASK_ARN in $TASK_ARNS; do
            aws ecs stop-task --cluster "$CLUSTER_NAME" --task "$TASK_ARN" --no-cli-pager
        done
        print_success "ECS tasks stopped"
    else
        print_info "No running ECS tasks"
    fi
else
    print_info "ECS cluster not found or not active"
fi

# Destroy CDK stacks
print_info "Destroying CDK stacks..."
print_info "This may take 15-20 minutes..."

npx cdk destroy --all --context environment=$ENVIRONMENT --force

if [ $? -ne 0 ]; then
    print_warning "Some stacks may have failed to delete. Check AWS Console for details."
    exit 1
fi

print_success "All stacks destroyed successfully!"

# Clean up any remaining resources (manual cleanup)
echo ""
print_info "Verifying cleanup..."

# Check for remaining CloudFormation stacks
REMAINING_STACKS=$(aws cloudformation list-stacks \
  --stack-status-filter CREATE_COMPLETE UPDATE_COMPLETE \
  --query "StackSummaries[?contains(StackName, 'rapid-photo-upload-${ENVIRONMENT}')].StackName" \
  --output text)

if [ -n "$REMAINING_STACKS" ]; then
    print_warning "Some CloudFormation stacks still exist:"
    echo "$REMAINING_STACKS"
else
    print_success "All CloudFormation stacks removed"
fi

# Final summary
echo ""
echo "========================================"
echo "Destruction Summary"
echo "========================================"
echo "Environment: $ENVIRONMENT"
echo ""
print_success "Infrastructure destroyed successfully!"
echo ""
print_info "If you encounter any issues, manually check AWS Console for:"
echo "  • CloudFormation stacks"
echo "  • S3 buckets"
echo "  • ECR repositories"
echo "  • RDS instances"
echo "  • VPCs"
echo ""
