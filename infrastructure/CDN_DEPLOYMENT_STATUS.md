# CDN Deployment Status

## Current Status

✅ **CloudFront Distribution**: Deployed and active
- URL: `https://d1zdygwm501yvh.cloudfront.net`
- Distribution ID: `E3E58UKNHBCLLJ`

⚠️ **ImageOAI**: Not yet created (blocked by CDK cyclic dependency)

⚠️ **S3 Bucket Policy**: Needs manual setup after ImageOAI is created

## Issue: CDK Cyclic Dependency

CDK detects a potential cyclic dependency when trying to:
1. Frontend stack depends on Storage stack (to get photo bucket)
2. Frontend stack tries to create ImageOAI and reference it
3. CDK thinks Storage stack might need ImageOAI, creating a cycle

**Workaround**: Create ImageOAI manually or use a different CDK pattern.

## Manual Setup Steps

### Option 1: Create ImageOAI via AWS CLI

```bash
# 1. Create CloudFront Origin Access Identity
OAI_OUTPUT=$(aws cloudfront create-cloud-front-origin-access-identity \
  --cloud-front-origin-access-identity-config \
    CallerReference="rapid-photo-upload-dev-image-oai-$(date +%s)",Comment="OAI for rapid-photo-upload-dev image delivery" \
  --output json)

OAI_ID=$(echo $OAI_OUTPUT | jq -r '.CloudFrontOriginAccessIdentity.Id')
OAI_CANONICAL_USER_ID=$(echo $OAI_OUTPUT | jq -r '.CloudFrontOriginAccessIdentity.S3CanonicalUserId')

echo "OAI ID: $OAI_ID"
echo "Canonical User ID: $OAI_CANONICAL_USER_ID"

# 2. Update CloudFront distribution to use ImageOAI for /images/* origin
# (This requires updating the distribution configuration - see Option 2)

# 3. Add bucket policy
BUCKET_NAME="rapid-photo-upload-dev-photos-971422717446"

# Get current policy
CURRENT_POLICY=$(aws s3api get-bucket-policy --bucket $BUCKET_NAME --output text 2>/dev/null || echo '{"Version":"2012-10-17","Statement":[]}')

# Add CloudFront statement
NEW_POLICY=$(echo "$CURRENT_POLICY" | jq --arg oai_id "$OAI_CANONICAL_USER_ID" --arg bucket "$BUCKET_NAME" '
  .Statement += [{
    "Sid": "AllowCloudFrontImageAccess",
    "Effect": "Allow",
    "Principal": {
      "AWS": "arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity " + $oai_id
    },
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::" + $bucket + "/*"
  }]
')

# Apply policy
echo "$NEW_POLICY" | aws s3api put-bucket-policy --bucket $BUCKET_NAME --policy file:///dev/stdin
```

### Option 2: Update CloudFront Distribution (Recommended)

The `/images/*` behavior needs to be added to the CloudFront distribution with the ImageOAI.

**Current State**: The frontend stack code includes `/images/*` behavior, but it may not be deployed yet due to the cyclic dependency issue.

**Solution**: 
1. Deploy frontend stack after fixing CDK code (see below)
2. Or manually update CloudFront distribution via AWS Console/CLI

## Next Steps

### Immediate Actions

1. **Test Current CDN Endpoint**:
   ```bash
   curl -I "https://d1zdygwm501yvh.cloudfront.net/images/uploads/3ee3362a-c269-4098-b49f-3bda10b0a017/1762722839004-integration-test.jpg"
   ```
   Currently returns HTML (SPA fallback) - needs ImageOAI setup

2. **Configure Backend** (can be done now):
   ```bash
   cd infrastructure/scripts
   CDN_URL=$(./get-cdn-url.sh dev)
   ./update-cdn-config.sh dev $CDN_URL true false
   ```

3. **Fix CDK Cyclic Dependency**:
   - Option A: Use CloudFormation custom resource
   - Option B: Remove frontend->storage dependency and use bucket name string
   - Option C: Create ImageOAI in storage stack instead

### Recommended Fix

Update `frontend-stack.ts` to use bucket name string instead of bucket object:

```typescript
// Instead of passing photoBucket object, use bucket name
const photoBucketName = photoBucket.bucketName;

// Create origin using bucket name
origin: new origins.S3Origin(photoBucketName, {
  originAccessIdentity: imageOriginAccessIdentity,
}),
```

Then manually add bucket policy via script after deployment.

## Testing Checklist

- [ ] ImageOAI created
- [ ] CloudFront distribution has `/images/*` behavior configured
- [ ] S3 bucket policy allows CloudFront OAI access
- [ ] CDN endpoint returns images (not HTML)
- [ ] Backend configured with CDN URL
- [ ] Backend generates CDN URLs in API responses
- [ ] Feature flag can be toggled

## Current Configuration

- **CDN URL**: `https://d1zdygwm501yvh.cloudfront.net`
- **Backend CDN Config**: Not yet configured (ready to configure)
- **Feature Flag**: Not enabled (ready to enable after testing)

