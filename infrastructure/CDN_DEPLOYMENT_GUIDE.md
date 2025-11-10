# CDN Image Delivery Deployment Guide

This guide provides step-by-step instructions for deploying and configuring the CloudFront-backed image CDN for RapidPhotoUpload.

## Prerequisites

- AWS CLI configured with appropriate credentials
- Node.js 18+ installed
- CDK dependencies installed (`npm install` in infrastructure directory)
- Existing compute stack deployed (for backend updates)

## Quick Start

### 1. Deploy CDN Infrastructure

Deploy the storage and frontend stacks that include CloudFront distribution:

```bash
cd infrastructure/scripts
./deploy-cdn.sh dev
```

This will:
- Deploy the storage stack (S3 bucket for photos)
- Deploy the frontend stack (CloudFront distribution + S3 website bucket)
- Display the CloudFront distribution URL

### 2. Configure Backend with CDN URL

After deployment, update the ECS task definition with CDN configuration:

```bash
# Get the CDN URL
CDN_URL=$(./get-cdn-url.sh dev)

# Update task definition (CDN enabled, feature flag disabled for testing)
./update-cdn-config.sh dev $CDN_URL true false
```

### 3. Enable Feature Flag (After Testing)

Once CDN endpoints are verified, enable the feature flag:

```bash
CDN_URL=$(./get-cdn-url.sh dev)
./update-cdn-config.sh dev $CDN_URL true true
```

## Detailed Deployment Steps

### Phase 1: Infrastructure Deployment

#### Step 1.1: Deploy Storage Stack

```bash
cd infrastructure
npx cdk deploy rapid-photo-upload-dev-storage --context environment=dev
```

**What this creates:**
- S3 bucket for photo storage
- Bucket policies for CloudFront access
- Lifecycle rules for cleanup

#### Step 1.2: Deploy Frontend Stack

```bash
npx cdk deploy rapid-photo-upload-dev-frontend --context environment=dev
```

**What this creates:**
- CloudFront distribution with `/images/*` behavior
- Origin Access Identity for private S3 bucket access
- Cache policies optimized for image delivery
- S3 bucket for static website hosting

**Key CloudFront Configuration:**
- `/images/*` path pattern routes to photo S3 bucket
- Query parameters supported: `w`, `h`, `fmt`, `q`
- Cache TTL: 7 days default, 30 days max
- Compression: Gzip and Brotli enabled

### Phase 2: Backend Configuration

#### Step 2.1: Retrieve CloudFront Distribution URL

```bash
CDN_URL=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-frontend \
  --query 'Stacks[0].Outputs[?OutputKey==`WebsiteUrl`].OutputValue' \
  --output text)

echo "CDN URL: $CDN_URL"
```

Or use the helper script:
```bash
./scripts/get-cdn-url.sh dev
```

#### Step 2.2: Update ECS Task Definition

The `update-cdn-config.sh` script automates this process, or you can do it manually:

**Manual Steps:**

1. Get current task definition:
```bash
CLUSTER_NAME=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`ClusterName`].OutputValue' \
  --output text)

SERVICE_NAME=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`ServiceName`].OutputValue' \
  --output text)

TASK_DEF_ARN=$(aws ecs describe-services \
  --cluster $CLUSTER_NAME \
  --services $SERVICE_NAME \
  --query 'services[0].taskDefinition' \
  --output text)
```

2. Export task definition:
```bash
aws ecs describe-task-definition \
  --task-definition $TASK_DEF_ARN \
  --query 'taskDefinition' > task-def.json
```

3. Add CDN environment variables to `task-def.json`:
```json
{
  "containerDefinitions": [{
    "environment": [
      ...existing variables...,
      {"name": "CDN_BASE_URL", "value": "https://d123.cloudfront.net"},
      {"name": "CDN_IMAGES_ENABLED", "value": "true"},
      {"name": "FEATURE_FLAG_IMAGE_CDN", "value": "false"}
    ]
  }]
}
```

4. Register new task definition and update service:
```bash
NEW_TASK_DEF_ARN=$(aws ecs register-task-definition \
  --cli-input-json file://task-def.json \
  --query 'taskDefinition.taskDefinitionArn' \
  --output text)

aws ecs update-service \
  --cluster $CLUSTER_NAME \
  --service $SERVICE_NAME \
  --task-definition $NEW_TASK_DEF_ARN \
  --force-new-deployment
```

### Phase 3: Testing & Validation

#### Step 3.1: Test CDN Endpoint

Test the CDN endpoint with a sample image:

```bash
# Replace <s3-key> with an actual photo S3 key from your bucket
curl -I "https://<cdn-domain>/images/<s3-key>?w=480&fmt=webp"
```

**Expected Response:**
- HTTP 200 status
- `Content-Type: image/webp` (or original format)
- `Cache-Control` header present
- `ETag` header present

#### Step 3.2: Verify Backend CDN URL Generation

Check backend logs to ensure CDN URLs are being generated:

```bash
aws logs tail /ecs/rapid-photo-upload-dev --follow | grep -i cdn
```

Look for:
- CDN URL generation in `PhotoResponseMapper`
- No errors related to CDN configuration

#### Step 3.3: Test API Response

Call the photos API and verify CDN URLs in response:

```bash
curl -H "Authorization: Bearer <token>" \
  https://<alb-dns>/api/photos | jq '.items[0].thumbnailVariants'
```

**Expected:**
- `thumbnailVariants.thumbnail` contains CDN URL
- URL includes query parameters (`w=480&q=75&fmt=webp`)
- URL points to CloudFront domain

### Phase 4: Feature Flag Activation

#### Step 4.1: Enable Feature Flag

After successful testing, enable the feature flag:

```bash
CDN_URL=$(./scripts/get-cdn-url.sh dev)
./scripts/update-cdn-config.sh dev $CDN_URL true true
```

#### Step 4.2: Verify Frontend Usage

1. Open browser DevTools Network tab
2. Load the photo gallery
3. Verify image requests go to CloudFront domain
4. Check response headers for cache hits

#### Step 4.3: Monitor Performance

- Check CloudFront metrics in AWS Console
- Monitor cache hit ratio (should be > 80% after warm-up)
- Verify image load times improved
- Check backend logs for any errors

## Environment Variables Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `CDN_IMAGES_ENABLED` | Master toggle for CDN URL generation | `false` | No |
| `CDN_BASE_URL` | CloudFront distribution URL | - | Yes (if enabled) |
| `CDN_DEFAULT_FORMAT` | Default image format | `webp` | No |
| `CDN_FEATURE_FLAG` | Feature flag key name | `image-cdn` | No |
| `FEATURE_FLAG_IMAGE_CDN` | Enable CDN URLs in API responses | `false` | No |
| `CDN_SIGNING_ENABLED` | Enable signed URLs | `false` | No |
| `CDN_KEY_PAIR_ID` | CloudFront key pair ID | - | Yes (if signing) |
| `CDN_PRIVATE_KEY_PEM` | RSA private key (PEM) | - | Yes (if signing) |
| `CDN_SIGNED_URL_TTL` | Signed URL TTL (ISO-8601) | `PT30M` | No |

## Troubleshooting

### CDN URLs Not Generated

**Symptoms:** API responses still contain S3 URLs

**Check:**
1. `CDN_IMAGES_ENABLED` is set to `true`
2. `CDN_BASE_URL` is set correctly
3. Backend logs show no CDN-related errors

**Solution:**
```bash
# Verify environment variables
aws ecs describe-task-definition \
  --task-definition <task-def-arn> \
  --query 'taskDefinition.containerDefinitions[0].environment'
```

### CloudFront Returns 403

**Symptoms:** CDN endpoint returns 403 Forbidden

**Check:**
1. S3 bucket policy includes Origin Access Identity
2. S3 object key is correct
3. CloudFront distribution is deployed and active

**Solution:**
```bash
# Verify bucket policy
aws s3api get-bucket-policy --bucket <bucket-name>

# Check CloudFront distribution status
aws cloudfront get-distribution --id <distribution-id> \
  --query 'Distribution.Status'
```

### Images Not Caching

**Symptoms:** Every request hits origin

**Check:**
1. Cache-Control headers from origin
2. Query parameters match cache policy allow list
3. CloudFront cache policy configuration

**Solution:**
- Verify cache policy includes `w`, `h`, `fmt`, `q` in query string allow list
- Check S3 object metadata for Cache-Control header

## Advanced: Signed URLs

To enable signed URLs for additional security:

### Step 1: Create CloudFront Key Pair

```bash
# Generate private key
openssl genrsa -out cloudfront-private-key.pem 2048

# Extract public key
openssl rsa -pubout -in cloudfront-private-key.pem -out cloudfront-public-key.pem
```

### Step 2: Upload Public Key to CloudFront

1. Go to AWS Console → CloudFront → Public Keys
2. Create new public key
3. Upload `cloudfront-public-key.pem`
4. Note the Key Pair ID

### Step 3: Create Key Group

1. Go to CloudFront → Key Groups
2. Create key group with the public key
3. Associate with distribution behavior `/images/*`

### Step 4: Configure Backend

Store private key in Secrets Manager:

```bash
aws secretsmanager create-secret \
  --name rapid-photo-upload-dev-cdn-signing-key \
  --secret-string file://cloudfront-private-key.pem
```

Update task definition to use secret:
```json
{
  "secrets": [
    {
      "name": "CDN_PRIVATE_KEY_PEM",
      "valueFrom": "arn:aws:secretsmanager:...:secret:rapid-photo-upload-dev-cdn-signing-key"
    }
  ]
}
```

Set environment variables:
- `CDN_SIGNING_ENABLED=true`
- `CDN_KEY_PAIR_ID=<key-pair-id>`

## Rollback Procedure

If issues occur, disable CDN feature flag:

```bash
CDN_URL=$(./scripts/get-cdn-url.sh dev)
./scripts/update-cdn-config.sh dev $CDN_URL true false
```

For complete rollback, disable CDN entirely:

```bash
./scripts/update-cdn-config.sh dev $CDN_URL false false
```

## Monitoring & Metrics

### CloudWatch Metrics

Monitor these CloudFront metrics:
- `4xxErrorRate` - Should be < 1%
- `5xxErrorRate` - Should be 0%
- `CacheHitRate` - Should be > 80%
- `BytesDownloaded` - Track data transfer costs
- `Requests` - Monitor traffic patterns

### Alarms

Set up CloudWatch alarms for:
- High error rates (> 5%)
- Low cache hit rate (< 50%)
- Unusual traffic spikes

## Cost Optimization

- **Cache Hit Rate:** Aim for > 80% to minimize origin requests
- **Price Class:** Use `PRICE_CLASS_100` (US, Canada, Europe) for lower costs
- **Compression:** Enable Brotli/Gzip to reduce data transfer
- **Cache TTL:** 7-day default balances freshness and cache efficiency

## Next Steps

After successful CDN rollout:
1. Monitor performance metrics for 1 week
2. Gather user feedback on image load times
3. Consider enabling AVIF format support
4. Implement Lambda@Edge for on-the-fly image optimization
5. Add analytics for variant usage patterns

