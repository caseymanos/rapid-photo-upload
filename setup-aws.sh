#!/bin/bash

echo "=========================================="
echo "RapidPhotoUpload - AWS S3 Setup"
echo "=========================================="
echo ""

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI is not installed"
    echo ""
    echo "Install it with:"
    echo "  brew install awscli"
    echo ""
    echo "Then run: aws configure"
    exit 1
fi

echo "✓ AWS CLI is installed"
echo ""

# Check if AWS is configured
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS credentials not configured"
    echo ""
    echo "Run: aws configure"
    echo ""
    echo "You'll need:"
    echo "  - AWS Access Key ID"
    echo "  - AWS Secret Access Key"
    echo "  - Default region (e.g., us-east-1)"
    exit 1
fi

echo "✓ AWS credentials configured"
echo ""

# Get AWS account info
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=$(aws configure get region)
echo "  Account: $AWS_ACCOUNT"
echo "  Region: $AWS_REGION"
echo ""

# Prompt for bucket name
DEFAULT_BUCKET="rapid-photo-upload-dev-$(whoami)"
echo "Enter S3 bucket name (press Enter for default):"
echo "Default: $DEFAULT_BUCKET"
read -p "Bucket name: " BUCKET_NAME

if [ -z "$BUCKET_NAME" ]; then
    BUCKET_NAME=$DEFAULT_BUCKET
fi

echo ""
echo "Creating bucket: $BUCKET_NAME"
echo ""

# Check if bucket exists
if aws s3 ls "s3://$BUCKET_NAME" 2>&1 | grep -q 'NoSuchBucket'; then
    # Create bucket
    echo "Creating S3 bucket..."

    if [ "$AWS_REGION" = "us-east-1" ]; then
        # us-east-1 doesn't need location constraint
        aws s3 mb "s3://$BUCKET_NAME"
    else
        # Other regions need location constraint
        aws s3 mb "s3://$BUCKET_NAME" --region "$AWS_REGION"
    fi

    if [ $? -eq 0 ]; then
        echo "✓ Bucket created successfully"
    else
        echo "❌ Failed to create bucket"
        exit 1
    fi
else
    echo "✓ Bucket already exists"
fi

echo ""

# Apply CORS configuration
echo "Applying CORS configuration..."
if [ -f "cors-config.json" ]; then
    aws s3api put-bucket-cors --bucket "$BUCKET_NAME" --cors-configuration file://cors-config.json

    if [ $? -eq 0 ]; then
        echo "✓ CORS configuration applied"
    else
        echo "❌ Failed to apply CORS configuration"
        exit 1
    fi
else
    echo "❌ cors-config.json not found"
    echo "Make sure you're running this script from the project root directory"
    exit 1
fi

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Your S3 bucket is ready:"
echo "  Name: $BUCKET_NAME"
echo "  Region: $AWS_REGION"
echo ""
echo "Next steps:"
echo "1. Update backend/.env.local with:"
echo "   S3_BUCKET_NAME=$BUCKET_NAME"
echo "   AWS_REGION=$AWS_REGION"
echo ""
echo "2. Get your AWS credentials:"
echo "   AWS Access Key ID: (from AWS Console → IAM → Your User → Security credentials)"
echo "   AWS Secret Access Key: (from AWS Console)"
echo ""
echo "3. Update backend/.env.local with your AWS credentials"
echo ""
echo "4. Generate JWT secret:"
echo "   openssl rand -base64 32"
echo ""
echo "5. Start the system:"
echo "   Terminal 1: docker start postgres-photoupload  # or docker run if first time"
echo "   Terminal 2: cd backend && ./run.sh"
echo "   Terminal 3: cd frontend-web && npm run dev"
echo ""
