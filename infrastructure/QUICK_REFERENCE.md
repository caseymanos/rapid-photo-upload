# RapidPhotoUpload - Quick Reference Guide

## Common Commands

### Initial Setup
```bash
cd infrastructure
npm install
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1
npx cdk bootstrap
```

### Deploy Everything (Automated)
```bash
# Development
./scripts/deploy.sh dev

# Production
./scripts/deploy.sh prod
```

### Manual Deployment Steps

#### 1. Deploy Infrastructure
```bash
# Dev
npm run deploy:dev

# Prod
npm run deploy:prod
```

#### 2. Build and Push Application
```bash
# Get ECR URI
export ECR_URI=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`RepositoryUri`].OutputValue' \
  --output text)

# Build and push
cd ../backend
docker build -t rapid-photo-upload:latest .
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI
docker tag rapid-photo-upload:latest $ECR_URI:latest
docker push $ECR_URI:latest
```

#### 3. Update ECS Service
```bash
aws ecs update-service \
  --cluster rapid-photo-upload-dev-cluster \
  --service rapid-photo-upload-dev-service \
  --force-new-deployment
```

### Monitoring

#### View Logs
```bash
# Tail logs
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Last 10 minutes
aws logs tail /ecs/rapid-photo-upload-dev --since 10m

# Filter for errors
aws logs tail /ecs/rapid-photo-upload-dev --follow --filter-pattern "ERROR"
```

#### Check Service Status
```bash
# ECS service
aws ecs describe-services \
  --cluster rapid-photo-upload-dev-cluster \
  --services rapid-photo-upload-dev-service

# Target health
aws elbv2 describe-target-health \
  --target-group-arn $(aws elbv2 describe-target-groups \
    --names rapid-photo-upload-dev-tg \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text)
```

#### Access Dashboard
```bash
# Get dashboard URL
aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-monitoring \
  --query 'Stacks[0].Outputs[?OutputKey==`DashboardUrl`].OutputValue' \
  --output text
```

### Database Operations

#### Get Database Credentials
```bash
aws secretsmanager get-secret-value \
  --secret-id rapid-photo-upload-dev-db-credentials \
  --query SecretString \
  --output text | jq .
```

#### Connect to Database (via ECS Exec)
```bash
# Get running task ID
TASK_ID=$(aws ecs list-tasks \
  --cluster rapid-photo-upload-dev-cluster \
  --service-name rapid-photo-upload-dev-service \
  --query 'taskArns[0]' \
  --output text | awk -F/ '{print $NF}')

# Connect to task
aws ecs execute-command \
  --cluster rapid-photo-upload-dev-cluster \
  --task $TASK_ID \
  --container rapid-photo-upload-dev \
  --interactive \
  --command "/bin/sh"

# Inside container, install psql and connect
apt-get update && apt-get install -y postgresql-client
psql -h $DATABASE_URL -U $DATABASE_USERNAME -d photoupload
```

### Scaling

#### Manual Scaling
```bash
# Set desired count
aws ecs update-service \
  --cluster rapid-photo-upload-dev-cluster \
  --service rapid-photo-upload-dev-service \
  --desired-count 3
```

#### Update Auto-Scaling Settings
Edit `lib/config.ts` and redeploy:
```typescript
ecsMinCapacity: 2,
ecsMaxCapacity: 8,
```

### Cost Management

#### Stop Development Environment
```bash
# Stop ECS tasks
aws ecs update-service \
  --cluster rapid-photo-upload-dev-cluster \
  --service rapid-photo-upload-dev-service \
  --desired-count 0

# Stop database (saves ~$15/month)
aws rds stop-db-instance \
  --db-instance-identifier rapid-photo-upload-dev-db
```

#### Start Development Environment
```bash
# Start database
aws rds start-db-instance \
  --db-instance-identifier rapid-photo-upload-dev-db

# Start ECS service
aws ecs update-service \
  --cluster rapid-photo-upload-dev-cluster \
  --service rapid-photo-upload-dev-service \
  --desired-count 1
```

### Troubleshooting

#### Task Won't Start
```bash
# Check task stopped reason
aws ecs describe-tasks \
  --cluster rapid-photo-upload-dev-cluster \
  --tasks $(aws ecs list-tasks \
    --cluster rapid-photo-upload-dev-cluster \
    --desired-status STOPPED \
    --query 'taskArns[0]' \
    --output text) \
  --query 'tasks[0].stoppedReason'

# Check CloudWatch logs
aws logs tail /ecs/rapid-photo-upload-dev --follow
```

#### ALB Health Checks Failing
```bash
# Check target health
aws elbv2 describe-target-health \
  --target-group-arn $(aws elbv2 describe-target-groups \
    --names rapid-photo-upload-dev-tg \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text)

# Test health endpoint directly (from inside VPC or via bastion)
curl http://<private-ip>:8080/actuator/health
```

#### Database Connection Issues
```bash
# Check database status
aws rds describe-db-instances \
  --db-instance-identifier rapid-photo-upload-dev-db \
  --query 'DBInstances[0].DBInstanceStatus'

# Check security groups
aws ec2 describe-security-groups \
  --group-names rapid-photo-upload-dev-db-sg
```

### Stack Updates

#### View Pending Changes
```bash
npm run diff
```

#### Deploy Specific Stack
```bash
npx cdk deploy rapid-photo-upload-dev-compute --context environment=dev
```

#### Rollback Failed Deployment
```bash
# Via CloudFormation
aws cloudformation cancel-update-stack \
  --stack-name rapid-photo-upload-dev-compute

# Then rollback
aws cloudformation continue-update-rollback \
  --stack-name rapid-photo-upload-dev-compute
```

### Cleanup

#### Destroy Everything
```bash
# Automated script
./scripts/destroy.sh dev

# Manual
npm run destroy
```

#### Destroy Specific Stack
```bash
npx cdk destroy rapid-photo-upload-dev-monitoring --context environment=dev
```

## Environment Variables Reference

### CDK Deployment
- `CDK_DEFAULT_ACCOUNT` - AWS account ID
- `CDK_DEFAULT_REGION` - AWS region (default: us-east-1)

### ECS Task Environment Variables (Auto-configured)
- `SPRING_PROFILES_ACTIVE` - dev or prod
- `AWS_REGION` - AWS region
- `S3_BUCKET_NAME` - S3 bucket name
- `EVENTBRIDGE_BUS_NAME` - EventBridge bus name
- `DATABASE_URL` - JDBC connection string
- `DATABASE_USERNAME` - From Secrets Manager
- `DATABASE_PASSWORD` - From Secrets Manager
- `JWT_SECRET` - From Secrets Manager

## Stack Outputs Quick Access

```bash
# Get all outputs for a stack
aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs' \
  --output table

# Get specific output
aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDnsName`].OutputValue' \
  --output text
```

## Cost Estimates

### Development (~$50-100/month)
- RDS t4g.micro: $15
- ECS Fargate (1 task): $25
- NAT Gateway: $30
- ALB: $20
- Other: $10

### Production (~$400-600/month)
- RDS r6g.large Multi-AZ: $200
- ECS Fargate (2-10 tasks): $100-300
- NAT Gateways (3): $90
- ALB: $20
- Other: variable

## Useful AWS Console Links

Replace `{region}` with your AWS region (e.g., us-east-1):

- **ECS Cluster**: `https://console.aws.amazon.com/ecs/home?region={region}#/clusters/rapid-photo-upload-dev-cluster`
- **CloudWatch Logs**: `https://console.aws.amazon.com/cloudwatch/home?region={region}#logsV2:log-groups/log-group/$252Fecs$252Frapid-photo-upload-dev`
- **CloudWatch Dashboard**: `https://console.aws.amazon.com/cloudwatch/home?region={region}#dashboards:name=rapid-photo-upload-dev`
- **RDS Instance**: `https://console.aws.amazon.com/rds/home?region={region}#database:id=rapid-photo-upload-dev-db`
- **S3 Bucket**: `https://s3.console.aws.amazon.com/s3/buckets/rapid-photo-upload-dev`
- **ALB**: `https://console.aws.amazon.com/ec2/home?region={region}#LoadBalancers:`

## Support Contacts

- **AWS Support**: https://console.aws.amazon.com/support/home
- **CDK GitHub**: https://github.com/aws/aws-cdk
- **Documentation**: See README.md and DEPLOYMENT_GUIDE.md
