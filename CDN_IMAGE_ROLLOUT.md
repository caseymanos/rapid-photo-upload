## CDN Image Optimization Rollout

This guide documents how to enable and validate the new CloudFront-backed image optimization layer. The rollout is gated by configuration so we can test safely before exposing the new URLs to all clients.

---

### 1. Architecture Changes

- `/images/*` is now served by CloudFront with an origin access identity for the private photo S3 bucket.
- Requests support responsive parameters (`w`, `h`, `q`, `fmt`) so CloudFront/Lambda@Edge (or future origin processors) can return optimized variants (AVIF/WebP).
- The backend switches from direct S3 thumbnails to CDN URLs through `ImageCdnUrlService`, behind the `image-cdn` feature flag.
- Signed URLs are supported via RSA private key; when configured, the backend appends CloudFront policy/signature parameters.

---

### 2. Required Configuration

Set the following environment variables for the backend (e.g., ECS task definition):

| Variable | Description |
| --- | --- |
| `CDN_IMAGES_ENABLED` | Master toggle. Set `true` to allow CDN URL generation. |
| `CDN_BASE_URL` | CloudFront distribution domain (e.g., `https://d123.cloudfront.net`). |
| `CDN_DEFAULT_FORMAT` | Preferred output format. Defaults to `webp`; you can set `avif` if transforms are supported. |
| `CDN_FEATURE_FLAG` | Feature flag key (`image-cdn`). Used to gradually enable the rollout. |
| `FEATURE_FLAG_IMAGE_CDN` | Set `true` to serve CDN URLs to clients. Keep `false` while testing. |
| `CDN_SIGNING_ENABLED` | Optional. Set `true` to require signed URLs. |
| `CDN_KEY_PAIR_ID` | CloudFront key pair identifier for the trusted key group. |
| `CDN_PRIVATE_KEY_PEM` | RSA private key in PEM format (single-line with `\n` escaped). |
| `CDN_SIGNED_URL_TTL` | ISO-8601 duration (e.g., `PT30M`) for signed URL validity. |

> NOTE: `CDN_IMAGES_ENABLED` must be `true` for the feature flag to have any effect.

**Infrastructure**

- Deploy the updated CDK stacks (`storage` then `frontend`) to create the new CloudFront behaviour and bucket policies.
- Ensure the CloudFront distribution trusts the key group associated with your signing key, if signing is enabled.

---

### 3. Gradual Rollout Plan

1. **Deploy infrastructure + backend** with `CDN_IMAGES_ENABLED=true` but keep `FEATURE_FLAG_IMAGE_CDN=false`.
2. **Smoke test** the CDN endpoints manually:
   - `curl "https://<cdn-domain>/images/<s3-key>?w=480&fmt=webp"`
   - Confirm 200 response, correct content type, and cache headers (`Cache-Control`, `ETag`).
3. **Verify signing (optional)** by temporarily enabling `CDN_SIGNING_ENABLED` and calling a signed URL. Ensure unsigned access returns 403.
4. **Enable feature flag on staging** (`FEATURE_FLAG_IMAGE_CDN=true`) for internal QA. Monitor logs for signature failures or slow responses.
5. **Front-end verification**:
   - Confirm galleries request `https://<cdn-domain>/images/...` instead of direct S3.
   - Use DevTools Network tab to ensure encoded parameters (`w`, `fmt`) and that `content-type` returns modern formats (AVIF/WebP) when supported by the browser.
6. **Performance testing**:
   - Run Lighthouse or WebPageTest before and after enabling the flag. Capture metrics for image weight and LCP.
   - Record cache hit rates via CloudFront metrics (`AWS/CloudFront`).
7. **Progressive rollout**:
   - Enable the feature flag for a small percentage of users (if using remote flag service) or specific accounts.
   - Monitor CloudFront costs and origin load.
8. **Full rollout**: Set `FEATURE_FLAG_IMAGE_CDN=true` (and keep `CDN_IMAGES_ENABLED=true`) once metrics look good.

---

### 4. Post-Rollout Checklist

- [ ] CloudFront behaviour `/images/*` returns HTTP/2 or HTTP/3 with `Cache-Control` >= 7 days.
- [ ] S3 bucket policies list the new Origin Access Identity principal.
- [ ] Backend logs show successful CDN URL generation without signature errors.
- [ ] Frontend/mobile clients load thumbnails via CDN and still fall back to presigned originals when needed.
- [ ] Cost monitoring alarms updated for CloudFront data transfer.

---

### 5. Testing Matrix

| Scenario | Expected Result |
| --- | --- |
| Browser supports AVIF | CDN returns `image/avif` with `fmt=avif` query parameter. |
| Browser without AVIF support | CDN falls back to WebP or original format. |
| Signed URL expired | CloudFront returns 403. Backend regenerates fresh URL on next request. |
| Feature flag disabled | API responses return legacy S3 thumbnail URLs. |
| Feature flag enabled | API responses include `thumbnailVariants.thumbnail` pointing to CDN. |

Run Lighthouse with throttling to capture LCP and total bytes. Store results in the Performance dashboard for regression tracking.

---

### 6. Operational Notes

- **Cache Invalidation**: Use `aws cloudfront create-invalidation --distribution-id <id> --paths "/images/*"` when updating the Lambda/image processor.
- **Purge policy**: Set automated purges for deleted photos to avoid stale CDN variants.
- **Monitoring**: Add alarms for CloudFront 5xx errors and origin latency. Review Athena logs (if enabled) for transformation failures.
- **Fallback Strategy**: If issues occur, flip `FEATURE_FLAG_IMAGE_CDN` to `false` (no redeploy required). For full disablement, set `CDN_IMAGES_ENABLED=false` and redeploy backend.

---

### 7. Next Steps

- Automate variant generation (Lambda@Edge or AWS Image Optimization) to guarantee AVIF/WebP availability.
- Add analytics on variant usage to refine quality settings (`q` parameter).
- Extend `PhotoResponse` to include `srcset` metadata for responsive `<img>` support.


