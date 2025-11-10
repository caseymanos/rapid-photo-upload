# RapidPhotoUpload - Step-by-Step Deployment Guide

This guide walks you through deploying the RapidPhotoUpload application to AWS from scratch.

## Prerequisites Checklist

- [ ] AWS account with administrator access
- [ ] AWS CLI v2 installed and configured
- [ ] Node.js 18.x or later installed
- [ ] Docker installed and running
- [ ] Maven 3.9+ installed (for local testing)
- [ ] Git repository cloned

## Phase 1: AWS Account Preparation

### Step 1: Configure AWS Credentials

```bash
# Configure AWS CLI
aws configure

# Verify configuration
aws sts get-caller-identity
```

Expected output:
```json
{
    "UserId": "AIDACKCEVSQ6C2EXAMPLE",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/your-username"
}
```

### Step 2: Set Environment Variables

```bash
# Set AWS account and region
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1

# Verify
echo "Account: $CDK_DEFAULT_ACCOUNT"
echo "Region: $CDK_DEFAULT_REGION"
```

### Step 3: Bootstrap CDK

This is a **one-time setup** per AWS account/region:

```bash
cd infrastructure
npm install

# Bootstrap CDK
npx cdk bootstrap aws://$CDK_DEFAULT_ACCOUNT/$CDK_DEFAULT_REGION
```

Expected output:
```
✅  Environment aws://123456789012/us-east-1 bootstrapped.
```

## Phase 2: Infrastructure Deployment

### Step 4: Review Configuration

Edit `infrastructure/lib/config.ts` if needed to customize:
- VPC CIDR ranges
- Database instance types
- ECS task sizes
- S3 bucket names
- CORS origins (important!)

For production, update CORS origins:
```typescript
corsOrigins: [
  'https://yourdomain.com',
  'https://www.yourdomain.com'
],
```

### Step 5: Synthesize CloudFormation Templates

```bash
cd infrastructure

# Generate CloudFormation templates
npm run synth
```

This creates `cdk.out/` directory with CloudFormation templates. Review them if desired.

### Step 6: Deploy Infrastructure (Development)

```bash
# Deploy all stacks to dev environment
npm run deploy:dev
```

**What happens:**
1. NetworkStack creates VPC, subnets, security groups (~5 minutes)
2. StorageStack creates S3 bucket (~1 minute)
3. EventStack creates EventBridge bus (~1 minute)
4. DatabaseStack creates RDS PostgreSQL (~10-15 minutes)
5. ComputeStack creates ECR, ECS cluster, ALB (~5 minutes)
6. MonitoringStack creates CloudWatch dashboard, alarms (~2 minutes)

**Total deployment time: ~20-30 minutes**

You'll be prompted to approve IAM role creation. Type `y` and press Enter.

### Step 7: Capture Stack Outputs

After deployment completes, save these outputs:

```bash
# Get all stack outputs
aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs' \
  --output table

# Save specific values
export ECR_URI=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`RepositoryUri`].OutputValue' \
  --output text)

export ALB_DNS=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDnsName`].OutputValue' \
  --output text)

echo "ECR Repository: $ECR_URI"
echo "Application URL: http://$ALB_DNS"
```

## Phase 3: Application Deployment

### Step 8: Build the Backend Application

```bash
cd ../backend

# Build with Maven (this creates the JAR file)
./mvnw clean package -DskipTests

# Verify JAR was created
ls -lh target/*.jar
```

Expected output:
```
-rw-r--r-- 1 user user 65M Nov 8 10:30 target/rapid-photo-upload-1.0.0.jar
```

### Step 9: Build Docker Image

```bash
# Still in backend directory
docker build -t rapid-photo-upload:latest .

# Verify image was created
docker images | grep rapid-photo-upload
```

Expected output:
```
rapid-photo-upload   latest   abc123def456   2 minutes ago   350MB
```

### Step 10: Push Image to ECR

```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region $CDK_DEFAULT_REGION | \
  docker login --username AWS --password-stdin $ECR_URI

# Tag image with ECR repository URI
docker tag rapid-photo-upload:latest $ECR_URI:latest

# Push to ECR
docker push $ECR_URI:latest
```

Expected output:
```
latest: digest: sha256:abc123... size: 3456
```

### Step 11: Deploy to ECS

The ECS service is already running but has no tasks (because no image exists yet). Now that we've pushed an image, update the service:

```bash
# Force new deployment
aws ecs update-service \
  --cluster rapid-photo-upload-dev-cluster \
  --service rapid-photo-upload-dev-service \
  --force-new-deployment \
  --region $CDK_DEFAULT_REGION
```

### Step 12: Wait for Deployment

```bash
# Watch task status
aws ecs describe-services \
  --cluster rapid-photo-upload-dev-cluster \
  --services rapid-photo-upload-dev-service \
  --query 'services[0].deployments' \
  --output table
```

Wait until:
- `runningCount` equals `desiredCount`
- Deployment status is `PRIMARY`
- `rolloutState` is `COMPLETED`

**This takes 2-5 minutes.**

### Step 13: Verify Application Health

```bash
# Check ALB target health
aws elbv2 describe-target-health \
  --target-group-arn $(aws elbv2 describe-target-groups \
    --names rapid-photo-upload-dev-tg \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text)

# Test health endpoint
curl http://$ALB_DNS/actuator/health
```

Expected output:
```json
{"status":"UP"}
```

### Step 14: Test the API

```bash
# Register a user
curl -X POST http://$ALB_DNS/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }'

# Login to get JWT token
TOKEN=$(curl -X POST http://$ALB_DNS/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }' | jq -r '.token')

echo "JWT Token: $TOKEN"

# Test authenticated endpoint
curl -X POST http://$ALB_DNS/api/uploads/initiate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "test.jpg",
    "fileSize": 1048576,
    "mimeType": "image/jpeg",
    "totalParts": 1
  }'
```

## Phase 4: Frontend Deployment (Optional)

### Option A: Deploy Frontend to S3 + CloudFront

```bash
cd ../frontend-web

# Build production bundle
npm run build

# Create S3 bucket for frontend
aws s3 mb s3://rapid-photo-upload-frontend-dev

# Upload build files
aws s3 sync dist/ s3://rapid-photo-upload-frontend-dev/ \
  --acl public-read

# Configure S3 for static website hosting
aws s3 website s3://rapid-photo-upload-frontend-dev/ \
  --index-document index.html \
  --error-document index.html
```

### Option B: Update Frontend to Use ALB DNS

```bash
cd ../frontend-web

# Update .env or vite.config.ts to use ALB DNS
echo "VITE_API_BASE_URL=http://$ALB_DNS" > .env.production

# Rebuild
npm run build
```

## Phase 5: Monitoring Setup

### Step 15: Configure CloudWatch Alarms

```bash
# Subscribe to alarm notifications
export ALARM_TOPIC_ARN=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-monitoring \
  --query 'Stacks[0].Outputs[?OutputKey==`AlarmTopicArn`].OutputValue' \
  --output text)

# Add email subscription
aws sns subscribe \
  --topic-arn $ALARM_TOPIC_ARN \
  --protocol email \
  --notification-endpoint your-email@example.com

# Check your email and confirm subscription
```

### Step 16: Access CloudWatch Dashboard

```bash
# Get dashboard URL
aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-monitoring \
  --query 'Stacks[0].Outputs[?OutputKey==`DashboardUrl`].OutputValue' \
  --output text
```

Open this URL in your browser to view metrics.

## Phase 6: Production Deployment

### Step 17: Deploy to Production

**Important:** Review and update production configuration in `lib/config.ts` first!

```bash
cd infrastructure

# Deploy to production
npm run deploy:prod
```

**Production deployment takes 30-45 minutes** due to Multi-AZ RDS and additional resources.

### Step 18: Configure Domain (Optional)

If you have a domain name:

1. **Create ACM certificate:**
   ```bash
   aws acm request-certificate \
     --domain-name yourdomain.com \
     --subject-alternative-names www.yourdomain.com \
     --validation-method DNS
   ```

2. **Add DNS validation records** in your domain registrar

3. **Update CDK configuration:**
   Edit `lib/config.ts` and set:
   ```typescript
   certificateArn: 'arn:aws:acm:us-east-1:123456789012:certificate/...'
   domainName: 'yourdomain.com'
   ```

4. **Redeploy:**
   ```bash
   npm run deploy:prod
   ```

5. **Create Route 53 alias record:**
   ```bash
   # Point yourdomain.com to ALB
   aws route53 change-resource-record-sets \
     --hosted-zone-id YOUR_ZONE_ID \
     --change-batch file://route53-changes.json
   ```

## Troubleshooting

### Issue: ECS Tasks Not Starting

**Check logs:**
```bash
aws logs tail /ecs/rapid-photo-upload-dev --follow
```

**Common causes:**
- Image not found in ECR → Push Docker image
- Database connection timeout → Check security groups
- Out of memory → Increase task memory in config

### Issue: ALB Returns 503

**Check target health:**
```bash
aws elbv2 describe-target-health \
  --target-group-arn <target-group-arn>
```

**Common causes:**
- No healthy targets → Check ECS task logs
- Health check failing → Verify `/actuator/health` endpoint
- Security group blocking traffic → Check ECS security group

### Issue: Database Connection Refused

**Verify database is running:**
```bash
aws rds describe-db-instances \
  --db-instance-identifier rapid-photo-upload-dev-db \
  --query 'DBInstances[0].DBInstanceStatus'
```

**Common causes:**
- Database still starting → Wait a few minutes
- Security group blocking → Check database security group allows ECS
- Wrong credentials → Check Secrets Manager

## Rollback Procedure

If deployment fails or application has critical issues:

```bash
# Rollback to previous ECS task definition
aws ecs update-service \
  --cluster rapid-photo-upload-dev-cluster \
  --service rapid-photo-upload-dev-service \
  --task-definition rapid-photo-upload-dev:PREVIOUS_REVISION

# Or destroy and redeploy stacks
npm run destroy
npm run deploy:dev
```

## Post-Deployment Checklist

- [ ] Application accessible via ALB URL
- [ ] Health check returns `{"status":"UP"}`
- [ ] User registration/login working
- [ ] Upload initiation returns presigned URLs
- [ ] CloudWatch dashboard showing metrics
- [ ] Alarm notifications configured
- [ ] Database backups enabled (check RDS console)
- [ ] S3 bucket CORS configured correctly
- [ ] Frontend connected to backend (if deployed)

## Next Steps

1. **Set up CI/CD pipeline** (see README.md for GitHub Actions example)
2. **Configure domain and SSL certificate** for production
3. **Implement backup strategy** for database and S3
4. **Set up log aggregation** (CloudWatch Insights, third-party tools)
5. **Performance testing** with 100 concurrent uploads
6. **Security audit** (AWS Trusted Advisor, third-party scans)

## Cost Monitoring

Set up billing alerts:

```bash
# Create billing alarm (requires us-east-1)
aws cloudwatch put-metric-alarm \
  --alarm-name rapid-photo-upload-billing-alert \
  --alarm-description "Alert when estimated charges exceed $100" \
  --metric-name EstimatedCharges \
  --namespace AWS/Billing \
  --statistic Maximum \
  --period 21600 \
  --evaluation-periods 1 \
  --threshold 100 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=Currency,Value=USD \
  --region us-east-1
```

## Support

For deployment issues:
1. Check CloudWatch logs: `/ecs/rapid-photo-upload-dev`
2. Review CloudFormation events in AWS Console
3. Verify all prerequisites are met
4. Consult AWS CDK documentation

---

**Deployment Complete!** Your RapidPhotoUpload application is now running on AWS.
