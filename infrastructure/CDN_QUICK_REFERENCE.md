# CDN Deployment Quick Reference

## One-Liner Deployment

```bash
# Deploy CDN infrastructure
cd infrastructure/scripts && ./deploy-cdn.sh dev

# Configure backend (after getting CDN URL)
CDN_URL=$(./get-cdn-url.sh dev) && ./update-cdn-config.sh dev $CDN_URL true false

# Enable feature flag (after testing)
./update-cdn-config.sh dev $CDN_URL true true
```

## Common Commands

### Get CDN URL
```bash
./scripts/get-cdn-url.sh dev
```

### Deploy CDN Infrastructure
```bash
./scripts/deploy-cdn.sh dev
```

### Update Backend Configuration
```bash
# Enable CDN, disable feature flag (testing mode)
./scripts/update-cdn-config.sh dev <cdn-url> true false

# Enable CDN and feature flag (production mode)
./scripts/update-cdn-config.sh dev <cdn-url> true true

# Disable CDN completely
./scripts/update-cdn-config.sh dev <cdn-url> false false
```

### Test CDN Endpoint
```bash
curl -I "https://<cdn-domain>/images/<s3-key>?w=480&fmt=webp"
```

### Check Backend Logs
```bash
aws logs tail /ecs/rapid-photo-upload-dev --follow | grep -i cdn
```

## Configuration States

| CDN Enabled | Feature Flag | Behavior |
|-------------|--------------|----------|
| `false` | `false` | Legacy S3 URLs (default) |
| `true` | `false` | CDN URLs generated but not used (testing) |
| `true` | `true` | CDN URLs active (production) |

## Environment Variables

Required for CDN:
- `CDN_BASE_URL` - CloudFront distribution URL
- `CDN_IMAGES_ENABLED` - Set to `true` to enable
- `FEATURE_FLAG_IMAGE_CDN` - Set to `true` to activate

Optional:
- `CDN_DEFAULT_FORMAT` - Default format (default: `webp`)
- `CDN_SIGNING_ENABLED` - Enable signed URLs (default: `false`)

## Troubleshooting Quick Fixes

**CDN not working?**
1. Check `CDN_BASE_URL` is set correctly
2. Verify `CDN_IMAGES_ENABLED=true`
3. Check backend logs for errors

**403 errors?**
1. Verify S3 bucket policy includes OAI
2. Check CloudFront distribution is active
3. Verify S3 object exists

**Images not caching?**
1. Check query parameters match cache policy
2. Verify cache policy configuration
3. Check CloudFront distribution status

## Rollback

```bash
# Disable feature flag (quick rollback)
./scripts/update-cdn-config.sh dev <cdn-url> true false

# Complete disable
./scripts/update-cdn-config.sh dev <cdn-url> false false
```

