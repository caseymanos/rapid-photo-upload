#!/bin/bash
# Script to configure S3 bucket policy for CloudFront access
# Fixes: Parse S3 bucket policy before appending statements

set -e

# Check for required arguments
if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: $0 <bucket-name> <cloudfront-oai-id>"
    echo "Example: $0 my-photo-bucket E2EXAMPLE123456"
    exit 1
fi

BUCKET_NAME="$1"
CLOUDFRONT_OAI_ID="$2"

echo "Configuring bucket policy for: $BUCKET_NAME"
echo "CloudFront OAI ID: $CLOUDFRONT_OAI_ID"

# Get current bucket policy
# aws s3api get-bucket-policy returns: { "Policy": "{...json string...}" }
POLICY_RESPONSE=$(aws s3api get-bucket-policy --bucket "$BUCKET_NAME" 2>/dev/null || echo "")

# Extract and parse the Policy field, or start with a default empty policy
if [ -z "$POLICY_RESPONSE" ]; then
    echo "No existing policy found. Creating new policy..."
    CURRENT_POLICY='{"Version":"2012-10-17","Statement":[]}'
else
    echo "Existing policy found. Parsing..."
    # Extract the Policy field (which is a JSON string) and parse it
    CURRENT_POLICY=$(echo "$POLICY_RESPONSE" | jq -r '.Policy' | jq .)
fi

# Ensure Statement is an array (initialize if null or missing)
CURRENT_POLICY=$(echo "$CURRENT_POLICY" | jq 'if .Statement == null then .Statement = [] else . end')

# Create the CloudFront OAI statement
CLOUDFRONT_STATEMENT=$(cat <<EOF
{
  "Sid": "AllowCloudFrontOAI",
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity $CLOUDFRONT_OAI_ID"
  },
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::$BUCKET_NAME/*"
}
EOF
)

# Check if statement already exists (by Sid)
EXISTING_STATEMENT=$(echo "$CURRENT_POLICY" | jq '.Statement[] | select(.Sid == "AllowCloudFrontOAI")' || echo "")

if [ -n "$EXISTING_STATEMENT" ]; then
    echo "CloudFront OAI statement already exists. Updating..."
    # Remove old statement and add new one
    NEW_POLICY=$(echo "$CURRENT_POLICY" | jq --argjson stmt "$CLOUDFRONT_STATEMENT" '
        .Statement = [.Statement[] | select(.Sid != "AllowCloudFrontOAI")] + [$stmt]
    ')
else
    echo "Adding new CloudFront OAI statement..."
    # Append the new statement
    NEW_POLICY=$(echo "$CURRENT_POLICY" | jq --argjson stmt "$CLOUDFRONT_STATEMENT" '
        .Statement += [$stmt]
    ')
fi

# Apply the updated policy
echo "Applying updated policy..."
echo "$NEW_POLICY" | aws s3api put-bucket-policy --bucket "$BUCKET_NAME" --policy file:///dev/stdin

echo "✓ Bucket policy updated successfully!"
echo ""
echo "Policy now includes CloudFront OAI access for: $CLOUDFRONT_OAI_ID"
