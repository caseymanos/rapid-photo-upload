# Why HTTP and How to Fix It

## Current Situation

Your infrastructure **is configured for HTTPS**, but it's not enabled because:

Looking at `lib/compute-stack.ts:176-186`:
```typescript
// Add HTTPS listener if certificate ARN is provided
if (config.certificateArn) {
  this.loadBalancer.addListener('HttpsListener', {
    port: 443,
    protocol: elbv2.ApplicationProtocol.HTTPS,
    certificates: [
      elbv2.ListenerCertificate.fromArn(config.certificateArn),
    ],
    defaultTargetGroups: [targetGroup],
  });
}
```

**The problem:** `config.certificateArn` is **undefined** (not set in `lib/config.ts`), so:
- ❌ No HTTPS listener is created
- ✅ Only HTTP listener on port 80 is active
- ⚠️ Your backend is exposed via `http://` only

## Security Implications

**What's at risk with HTTP:**
1. 🔓 **Passwords transmitted in plain text** - Anyone on the network can read them
2. 🔓 **JWT tokens visible** - Session hijacking is trivial
3. 🔓 **Photo uploads unencrypted** - Privacy violation
4. 🔓 **Man-in-the-middle attacks** - Attacker can modify requests/responses
5. ❌ **iOS App Store rejection** - Apple requires HTTPS for production apps
6. ❌ **Browser warnings** - Modern browsers flag HTTP as "Not Secure"

## Solution: Add SSL Certificate

You need to:
1. Get an SSL certificate for your domain
2. Update CDK config with certificate ARN
3. Redeploy infrastructure
4. Update mobile app URLs to use `https://`

## Step-by-Step HTTPS Setup

### Option 1: Use AWS Certificate Manager (ACM) - FREE & Recommended

#### Prerequisites
You need a domain name. Options:
- **Buy a domain**: Route53, Namecheap, GoDaddy (~$10-15/year)
- **Use a free subdomain**: Use services like FreeDNS, Afraid.org
- **For testing**: Use AWS-provided ALB domain (see Option 3 below)

#### Step 1: Request SSL Certificate

```bash
# Request certificate for your domain
aws acm request-certificate \
  --region us-east-1 \
  --domain-name rapidphotoupload.yourdomain.com \
  --validation-method DNS \
  --idempotency-token rapid-photo-upload-cert
```

This returns a Certificate ARN like:
```
arn:aws:acm:us-east-1:971422717446:certificate/abc123...
```

#### Step 2: Validate Domain Ownership

```bash
# Get validation records
aws acm describe-certificate \
  --region us-east-1 \
  --certificate-arn arn:aws:acm:us-east-1:971422717446:certificate/YOUR_CERT_ID
```

Add the CNAME records to your DNS provider (Route53, Namecheap, etc.)

Wait for validation (usually 5-30 minutes):
```bash
# Check status
aws acm describe-certificate \
  --region us-east-1 \
  --certificate-arn arn:aws:acm:us-east-1:971422717446:certificate/YOUR_CERT_ID \
  --query 'Certificate.Status'
```

#### Step 3: Update CDK Configuration

Edit `infrastructure/lib/config.ts`:

```typescript
dev: {
  environment: 'dev',
  appName: 'rapid-photo-upload',
  // ... existing config ...

  // Add this:
  certificateArn: 'arn:aws:acm:us-east-1:971422717446:certificate/YOUR_CERT_ID',
},
```

#### Step 4: Redeploy Infrastructure

```bash
cd infrastructure
npx cdk deploy rapid-photo-upload-dev-compute
```

This will:
- ✅ Add HTTPS listener on port 443
- ✅ Keep HTTP listener on port 80 (you can redirect HTTP→HTTPS if desired)
- ✅ Attach SSL certificate to ALB

#### Step 5: Update DNS

Point your domain to the ALB:
```bash
# Get ALB DNS name
aws elbv2 describe-load-balancers \
  --region us-east-1 \
  --query 'LoadBalancers[?contains(LoadBalancerName, `rapid-photo-upload-dev`)].DNSName' \
  --output text
```

In your DNS provider, create a CNAME record:
```
rapidphotoupload.yourdomain.com  →  rapid-photo-upload-dev-alb-xxxxx.us-east-1.elb.amazonaws.com
```

#### Step 6: Update Mobile App URLs

Update `mobile-app/eas.json`:
```json
{
  "build": {
    "preview": {
      "env": {
        "API_BASE_URL": "https://rapidphotoupload.yourdomain.com/api/v1",
        "WS_URL": "wss://rapidphotoupload.yourdomain.com/ws"
      }
    }
  }
}
```

Update `mobile-app/app.config.js`:
```javascript
extra: {
  apiUrl: "https://rapidphotoupload.yourdomain.com/api/v1",
  wsUrl: "wss://rapidphotoupload.yourdomain.com/ws",
  // ...
}
```

#### Step 7: Remove iOS ATS Exception

In `mobile-app/app.config.js`, remove the `NSAppTransportSecurity` section since you now have HTTPS.

#### Step 8: Rebuild Mobile App

```bash
cd mobile-app
eas build --platform ios --profile preview
```

---

### Option 2: Use CloudFront + ALB (Better Performance)

CloudFront gives you:
- ✅ Free AWS SSL certificate
- ✅ CDN caching (faster for users)
- ✅ DDoS protection
- ✅ No domain required (use CloudFront URL)

**Trade-off:** More complex setup, extra service to manage

See AWS docs: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/distribution-web-values-specify.html

---

### Option 3: Use ALB DNS Directly with Self-Signed Cert (Development Only)

**For quick testing only** (not for production):

1. Create self-signed certificate
2. Import to ACM
3. Accept security warnings in browsers/apps

This is **NOT recommended** because:
- ❌ iOS will require accepting invalid certificates
- ❌ Browsers show scary warnings
- ❌ Not secure against MITM attacks

---

## Recommended Approach

**For Development/Testing:**
1. Buy a cheap domain ($10-15/year) like `rapidphotoupload.com`
2. Use AWS Certificate Manager (FREE)
3. Set up HTTPS properly
4. Your app works securely everywhere

**Total cost:** ~$10-15/year for domain (certificate is FREE with ACM)

**For Production:**
Same setup, but use the production CDK config.

---

## Quick Commands Summary

```bash
# 1. Request certificate
aws acm request-certificate \
  --region us-east-1 \
  --domain-name your-domain.com \
  --validation-method DNS

# 2. Check status
aws acm describe-certificate --certificate-arn YOUR_ARN

# 3. Update config
# Edit infrastructure/lib/config.ts

# 4. Deploy
cd infrastructure
npx cdk deploy rapid-photo-upload-dev-compute

# 5. Get ALB DNS
aws elbv2 describe-load-balancers --query 'LoadBalancers[0].DNSName'

# 6. Update mobile app
# Edit mobile-app/eas.json and mobile-app/app.config.js

# 7. Rebuild
cd mobile-app
eas build --platform ios --profile preview
```

---

## Why This Wasn't Set Up Initially

Your infrastructure code **already supports HTTPS** - it just needs a certificate ARN. This is a common pattern because:

1. **Certificate requires domain validation** - Can't automate without a domain
2. **Development flexibility** - HTTP works fine for local testing
3. **Cost consideration** - Domain costs money (though certificates are free)

The infrastructure was designed to add HTTPS easily by just setting `certificateArn` in the config - exactly as intended!

---

## Next Steps

1. **Decide on domain** (buy one or use subdomain service)
2. **Request ACM certificate**
3. **Update CDK config** with certificate ARN
4. **Redeploy** infrastructure
5. **Update mobile app** URLs to HTTPS
6. **Rebuild app** with HTTPS

**Time estimate:** 30-60 minutes (mostly waiting for certificate validation)

**Cost:** $10-15/year for domain name

Want help with any of these steps?
