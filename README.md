# Rapid Photo Upload

Scripts and utilities for rapid photo upload infrastructure using AWS S3 and CloudFront.

## Scripts

### setup-s3-policy.sh

Configures S3 bucket policy to allow CloudFront Origin Access Identity (OAI) access.

**Usage:**
```bash
./setup-s3-policy.sh <bucket-name> <cloudfront-oai-id>
```

**Example:**
```bash
./setup-s3-policy.sh my-photo-bucket E2EXAMPLE123456
```

**Features:**
- ✓ Properly parses existing S3 bucket policies (handles the `{ "Policy": "..." }` response format)
- ✓ Initializes empty policy structure when no policy exists
- ✓ Safely appends CloudFront OAI statement to existing policies
- ✓ Updates existing CloudFront statement if already present
- ✓ Handles null or missing Statement arrays

**Requirements:**
- AWS CLI configured with appropriate credentials
- `jq` command-line JSON processor
- Permissions to read and write bucket policies

## Issue Fixes

### Issue #4: Parse S3 bucket policy before appending statements

The script correctly handles the S3 API response format:
1. `aws s3api get-bucket-policy` returns `{ "Policy": "{...json string...}" }`
2. The script extracts the Policy field with `jq -r '.Policy'`
3. The JSON string is then parsed into an object
4. Handles cases where no policy exists (empty response)
5. Initializes `.Statement` array if null or missing
6. Only then appends the CloudFront statement

This prevents the "Cannot iterate over null" error that occurred when trying to access `.Statement` directly on the unparsed response.

## License

MIT License - see [LICENSE](LICENSE) file for details.
