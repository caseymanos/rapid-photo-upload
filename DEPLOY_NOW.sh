#!/bin/bash
# RapidPhotoUpload - Quick Deployment Script
# Run this script to deploy the entire production system

set -e  # Exit on error

echo "=========================================="
echo "RapidPhotoUpload Production Deployment"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if in correct directory
if [ ! -f "DEPLOYMENT_READY.md" ]; then
    echo -e "${RED}❌ Error: Please run this script from the project root directory${NC}"
    exit 1
fi

# Verify AWS credentials
echo -e "${BLUE}Step 1: Verifying AWS credentials...${NC}"
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    echo -e "${RED}❌ Error: AWS credentials not configured${NC}"
    echo "Run: aws configure"
    exit 1
fi
echo -e "${GREEN}✅ AWS credentials verified${NC}"
echo ""

# Set environment variables
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1

echo "AWS Account: $CDK_DEFAULT_ACCOUNT"
echo "AWS Region: $CDK_DEFAULT_REGION"
echo ""

# Ask user what to deploy
echo "What would you like to deploy?"
echo "1. Frontend only (CloudFront + S3)"
echo "2. Mobile app only (Expo EAS)"
echo "3. Full stack (Frontend + Mobile)"
echo ""
read -p "Enter choice (1-3): " choice

case $choice in
    1)
        echo -e "${BLUE}Deploying Frontend to CloudFront...${NC}"
        
        # Build frontend
        echo "Building React app..."
        cd frontend-web
        npm install
        npm run build
        
        # Deploy CDK stack
        echo "Deploying CDK stack..."
        cd ../infrastructure
        cdk deploy rapid-photo-upload-dev-frontend --require-approval never
        
        echo -e "${GREEN}✅ Frontend deployed successfully!${NC}"
        echo ""
        echo "CloudFront URL will be shown in the CDK output above."
        echo "Look for: rapid-photo-upload-dev-frontend.WebsiteUrl"
        ;;
        
    2)
        echo -e "${BLUE}Deploying Mobile App via Expo EAS...${NC}"
        
        cd mobile-app
        
        # Check if EAS CLI is installed
        if ! command -v eas &> /dev/null; then
            echo "Installing EAS CLI..."
            npm install -g eas-cli
        fi
        
        # Check if logged in
        echo "Please login to Expo (if not already logged in):"
        eas login
        
        # Initialize if needed
        if [ ! -f ".easrc" ]; then
            echo "Initializing EAS project..."
            eas init
        fi
        
        # Build
        echo "Building mobile app (this may take 10-15 minutes)..."
        eas build --platform all --profile development
        
        echo -e "${GREEN}✅ Mobile app build started!${NC}"
        echo "Check build status at: https://expo.dev"
        ;;
        
    3)
        echo -e "${BLUE}Deploying Full Stack...${NC}"
        
        # Frontend
        echo -e "${BLUE}1/2: Building and deploying frontend...${NC}"
        cd frontend-web
        npm install
        npm run build
        cd ../infrastructure
        cdk deploy rapid-photo-upload-dev-frontend --require-approval never
        
        # Mobile
        echo -e "${BLUE}2/2: Building mobile app...${NC}"
        cd ../mobile-app
        
        if ! command -v eas &> /dev/null; then
            npm install -g eas-cli
        fi
        
        eas login
        
        if [ ! -f ".easrc" ]; then
            eas init
        fi
        
        eas build --platform all --profile development
        
        echo -e "${GREEN}✅ Full stack deployment complete!${NC}"
        ;;
        
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Test the deployed application"
echo "2. Check CloudWatch logs for any errors"
echo "3. Run performance tests (100 concurrent uploads)"
echo ""
echo "For detailed testing instructions, see:"
echo "  PRODUCTION_DEPLOYMENT_GUIDE.md"
echo ""
