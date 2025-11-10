#!/bin/bash

# HTTPS Setup Script for RapidPhotoUpload
# This script helps you set up HTTPS with AWS Certificate Manager

set -e

echo "🔒 RapidPhotoUpload HTTPS Setup"
echo "================================"
echo ""

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI not found. Please install it first."
    exit 1
fi

# Check if logged in
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ Not logged into AWS. Run: aws configure"
    exit 1
fi

REGION="us-east-1"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo "✅ AWS Account: $ACCOUNT_ID"
echo "✅ Region: $REGION"
echo ""

# Step 1: Get domain name
echo "Step 1: Domain Setup"
echo "===================="
echo ""
echo "Do you have a domain name? (yes/no)"
echo ""
echo "Options:"
echo "  1. Buy a domain (~\$10-15/year):"
echo "     - Route53: https://console.aws.amazon.com/route53/home#DomainRegistration:"
echo "     - Namecheap: https://www.namecheap.com"
echo "     - GoDaddy: https://www.godaddy.com"
echo ""
echo "  2. Use existing domain"
echo ""
echo "  3. Skip (continue with HTTP for now)"
echo ""

read -p "Enter your choice (1/2/3): " choice

case $choice in
  1)
    echo ""
    echo "📝 After purchasing a domain, come back and run this script again"
    echo ""
    exit 0
    ;;
  2)
    read -p "Enter your domain name (e.g., rapidphotoupload.com): " DOMAIN
    echo ""
    echo "📝 You entered: $DOMAIN"
    echo ""
    ;;
  3)
    echo ""
    echo "⚠️  Continuing with HTTP"
    echo ""
    echo "To enable HTTPS later:"
    echo "  1. Get a domain"
    echo "  2. Run this script again"
    echo ""
    exit 0
    ;;
  *)
    echo "Invalid choice"
    exit 1
    ;;
esac

# Step 2: Request certificate
echo "Step 2: Request SSL Certificate"
echo "================================"
echo ""
echo "Requesting certificate for: $DOMAIN"
echo ""

CERT_ARN=$(aws acm request-certificate \
  --region $REGION \
  --domain-name "$DOMAIN" \
  --validation-method DNS \
  --idempotency-token rapid-photo-upload-https \
  --query 'CertificateArn' \
  --output text)

if [ $? -eq 0 ]; then
  echo "✅ Certificate requested: $CERT_ARN"
else
  echo "❌ Failed to request certificate"
  exit 1
fi

echo ""
echo "Step 3: Domain Validation"
echo "========================="
echo ""
echo "You need to add a CNAME record to your DNS to validate the domain."
echo ""

# Get validation records
sleep 2
aws acm describe-certificate \
  --region $REGION \
  --certificate-arn "$CERT_ARN" \
  --query 'Certificate.DomainValidationOptions[0].ResourceRecord' \
  --output table

echo ""
echo "📝 Instructions:"
echo "1. Go to your DNS provider (Route53, Namecheap, GoDaddy, etc.)"
echo "2. Add the CNAME record shown above"
echo "3. Wait for validation (5-30 minutes)"
echo ""
echo "To check validation status, run:"
echo "  aws acm describe-certificate --region $REGION --certificate-arn \"$CERT_ARN\" --query 'Certificate.Status'"
echo ""
echo "When status shows 'ISSUED', continue to Step 4"
echo ""

read -p "Press Enter when you've added the DNS record and certificate is ISSUED..."

# Check if certificate is issued
STATUS=$(aws acm describe-certificate \
  --region $REGION \
  --certificate-arn "$CERT_ARN" \
  --query 'Certificate.Status' \
  --output text)

if [ "$STATUS" != "ISSUED" ]; then
  echo ""
  echo "⚠️  Certificate status: $STATUS"
  echo ""
  echo "Wait for validation to complete, then run these commands:"
  echo ""
  echo "  # Update config with certificate ARN"
  echo "  echo \"Certificate ARN: $CERT_ARN\""
  echo ""
  echo "  # Add to infrastructure/lib/config.ts in dev config:"
  echo "  certificateArn: '$CERT_ARN',"
  echo ""
  echo "  # Deploy infrastructure"
  echo "  cd infrastructure && npx cdk deploy rapid-photo-upload-dev-compute"
  echo ""
  exit 0
fi

echo ""
echo "✅ Certificate is ISSUED!"
echo ""

# Step 4: Update CDK config
echo "Step 4: Update Infrastructure Config"
echo "====================================="
echo ""
echo "Add this to infrastructure/lib/config.ts in the 'dev' config:"
echo ""
echo "  certificateArn: '$CERT_ARN',"
echo ""

read -p "Press Enter after updating config.ts..."

# Step 5: Deploy infrastructure
echo ""
echo "Step 5: Deploy Infrastructure"
echo "============================="
echo ""
echo "Deploying infrastructure with HTTPS..."
echo ""

cd "$(dirname "$0")"
npx cdk deploy rapid-photo-upload-dev-compute --require-approval never

if [ $? -eq 0 ]; then
  echo ""
  echo "✅ Infrastructure deployed with HTTPS!"
else
  echo ""
  echo "❌ Deployment failed"
  exit 1
fi

# Step 6: Get ALB DNS
echo ""
echo "Step 6: Update DNS"
echo "=================="
echo ""

ALB_DNS=$(aws elbv2 describe-load-balancers \
  --region $REGION \
  --query 'LoadBalancers[?contains(LoadBalancerName, `rapid-photo-upload-dev`)].DNSName' \
  --output text)

echo "ALB DNS Name: $ALB_DNS"
echo ""
echo "📝 In your DNS provider, create a CNAME record:"
echo ""
echo "  Name:   $DOMAIN"
echo "  Type:   CNAME"
echo "  Value:  $ALB_DNS"
echo ""

read -p "Press Enter after creating the CNAME record..."

# Step 7: Update mobile app
echo ""
echo "Step 7: Update Mobile App"
echo "========================="
echo ""
echo "Update these files:"
echo ""
echo "1. mobile-app/eas.json:"
echo "   \"API_BASE_URL\": \"https://$DOMAIN/api/v1\","
echo "   \"WS_URL\": \"wss://$DOMAIN/ws\""
echo ""
echo "2. mobile-app/app.config.js:"
echo "   apiUrl: \"https://$DOMAIN/api/v1\","
echo "   wsUrl: \"wss://$DOMAIN/ws\","
echo ""
echo "3. Remove NSAppTransportSecurity section from app.config.js"
echo ""

read -p "Press Enter after updating mobile app config..."

# Step 8: Test
echo ""
echo "Step 8: Test HTTPS"
echo "=================="
echo ""
echo "Testing HTTPS connection..."
echo ""

if curl -s -o /dev/null -w "%{http_code}" "https://$DOMAIN/actuator/health" | grep -q "200"; then
  echo "✅ HTTPS is working!"
else
  echo "⚠️  HTTPS test failed. This might be normal if DNS hasn't propagated yet."
  echo "   Wait a few minutes and test manually: https://$DOMAIN/actuator/health"
fi

echo ""
echo "🎉 HTTPS Setup Complete!"
echo "========================"
echo ""
echo "Next steps:"
echo "1. Rebuild your mobile app:"
echo "   cd mobile-app && eas build --platform ios --profile preview"
echo ""
echo "2. Test the app with HTTPS backend"
echo ""
echo "3. Your data is now encrypted! 🔒"
echo ""
echo "Certificate ARN: $CERT_ARN"
echo "Domain: https://$DOMAIN"
echo ""
