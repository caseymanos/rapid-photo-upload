#!/bin/bash

# Script to apply CDK deployment IAM policy
# This must be run by an AWS administrator with IAM permissions

set -e

POLICY_NAME="RapidPhotoUploadCDKDeployment"
USER_NAME="handwriting-math"
ACCOUNT_ID="971422717446"

echo "=========================================="
echo "CDK Deployment Policy Setup"
echo "=========================================="
echo ""
echo "This script will create and attach an IAM policy for CDK deployments."
echo "User: $USER_NAME"
echo "Account: $ACCOUNT_ID"
echo ""

# Check if running as administrator
echo "Checking AWS credentials..."
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ Error: AWS credentials not configured"
    exit 1
fi

CURRENT_USER=$(aws sts get-caller-identity --query 'Arn' --output text)
echo "Running as: $CURRENT_USER"
echo ""

# Create the policy
echo "Step 1: Creating IAM policy '$POLICY_NAME'..."
POLICY_ARN=$(aws iam create-policy \
    --policy-name "$POLICY_NAME" \
    --policy-document file://cdk-deployment-policy.json \
    --description "Permissions for deploying RapidPhotoUpload CDK infrastructure" \
    --query 'Policy.Arn' \
    --output text 2>&1)

if [[ $? -eq 0 ]]; then
    echo "✅ Policy created: $POLICY_ARN"
else
    # Check if policy already exists
    if echo "$POLICY_ARN" | grep -q "EntityAlreadyExists"; then
        echo "⚠️  Policy already exists, using existing policy..."
        POLICY_ARN="arn:aws:iam::$ACCOUNT_ID:policy/$POLICY_NAME"

        # Update the policy
        echo "Updating existing policy..."
        DEFAULT_VERSION=$(aws iam get-policy --policy-arn "$POLICY_ARN" --query 'Policy.DefaultVersionId' --output text)

        # Create new version
        aws iam create-policy-version \
            --policy-arn "$POLICY_ARN" \
            --policy-document file://cdk-deployment-policy.json \
            --set-as-default

        # Delete old version if not v1
        if [[ "$DEFAULT_VERSION" != "v1" ]]; then
            aws iam delete-policy-version \
                --policy-arn "$POLICY_ARN" \
                --version-id "$DEFAULT_VERSION"
        fi

        echo "✅ Policy updated: $POLICY_ARN"
    else
        echo "❌ Error creating policy: $POLICY_ARN"
        exit 1
    fi
fi
echo ""

# Attach the policy to the user
echo "Step 2: Attaching policy to user '$USER_NAME'..."
if aws iam attach-user-policy \
    --user-name "$USER_NAME" \
    --policy-arn "$POLICY_ARN"; then
    echo "✅ Policy attached successfully"
else
    echo "❌ Error attaching policy"
    exit 1
fi
echo ""

# Verify attachment
echo "Step 3: Verifying policy attachment..."
if aws iam list-attached-user-policies \
    --user-name "$USER_NAME" \
    --query "AttachedPolicies[?PolicyName=='$POLICY_NAME']" \
    --output table; then
    echo "✅ Policy verified"
else
    echo "⚠️  Warning: Could not verify policy attachment"
fi
echo ""

echo "=========================================="
echo "✅ Setup Complete!"
echo "=========================================="
echo ""
echo "The user '$USER_NAME' now has permissions to deploy CDK infrastructure."
echo ""
echo "Next steps:"
echo "1. Wait ~60 seconds for IAM changes to propagate"
echo "2. Run: npx cdk bootstrap"
echo "3. Run: npx cdk deploy --all --context environment=dev"
echo ""
