# RapidPhotoUpload - AWS CDK Infrastructure

This directory contains the AWS CDK (Cloud Development Kit) infrastructure as code for deploying the RapidPhotoUpload application to AWS.

## Architecture Overview

The infrastructure is organized into multiple CDK stacks:

1. **NetworkStack** - VPC with public/private subnets, NAT gateways, and security groups
2. **StorageStack** - S3 bucket with CORS configuration for photo storage
3. **EventStack** - EventBridge event bus for domain events
4. **DatabaseStack** - RDS PostgreSQL database with Secrets Manager integration
5. **ComputeStack** - ECR repository, ECS Fargate cluster, ALB, and auto-scaling
6. **MonitoringStack** - CloudWatch dashboards, alarms, and SNS notifications

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Internet                                  │
└──────────────────────┬──────────────────────────────────────────┘
                       │
              ┌────────▼─────────┐
              │   CloudFront     │ (Optional - for static assets)
              │   Distribution   │
              └────────┬─────────┘
                       │
              ┌────────▼─────────┐
              │  Application     │
              │  Load Balancer   │ (Public Subnets)
              └────────┬─────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼────────┐          ┌─────────▼────────┐
│  ECS Fargate   │          │  ECS Fargate     │
│  Task (AZ 1)   │          │  Task (AZ 2)     │ (Private Subnets)
└───────┬────────┘          └─────────┬────────┘
        │                             │
        └──────────────┬──────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
┌───────▼─────┐ ┌──────▼──────┐ ┌────▼─────────┐
│ RDS         │ │ S3 Bucket   │ │ EventBridge  │
│ PostgreSQL  │ │ (Photos)    │ │ Event Bus    │
│ Multi-AZ    │ │             │ │              │
└─────────────┘ └─────────────┘ └──────────────┘
 (Database         (Storage)      (Events)
  Subnets)
```

## Prerequisites

### Required Software

- **Node.js** 18.x or later
- **AWS CLI** v2 configured with credentials
- **AWS CDK** v2.120.0 or later
- **Docker** (for building container images)

### AWS Account Setup

1. **Configure AWS credentials:**
   ```bash
   aws configure
   # Or use environment variables:
   export AWS_ACCESS_KEY_ID=your-access-key
   export AWS_SECRET_ACCESS_KEY=your-secret-key
   export AWS_DEFAULT_REGION=us-east-1
   ```

2. **Set CDK environment variables:**
   ```bash
   export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
   export CDK_DEFAULT_REGION=us-east-1
   ```

3. **Bootstrap CDK in your AWS account** (one-time setup):
   ```bash
   npm run cdk bootstrap
   ```

## Installation

```bash
cd infrastructure
npm install
```

## Configuration

Configuration is managed in `lib/config.ts`. The configuration automatically adapts based on the environment:

### Development Environment (`dev`)
- **VPC:** 10.1.0.0/16, 2 AZs, 1 NAT Gateway
- **Database:** t4g.micro, 20GB storage, single-AZ
- **ECS:** 1 task (1 vCPU, 2GB RAM), auto-scale 1-4
- **EventBridge:** Default event bus
- **CORS:** Localhost origins

### Production Environment (`prod`)
- **VPC:** 10.0.0.0/16, 3 AZs, 3 NAT Gateways
- **Database:** r6g.large, 100GB storage, Multi-AZ
- **ECS:** 2 tasks (2 vCPU, 4GB RAM), auto-scale 2-10
- **EventBridge:** Custom event bus with archiving
- **CORS:** Production domains

To modify configuration, edit `lib/config.ts`.

## Deployment

### Build and Deploy All Stacks

**Development:**
```bash
npm run deploy:dev
```

**Production:**
```bash
npm run deploy:prod
```

### Deploy Individual Stacks

```bash
# Synthesize CloudFormation templates
npm run synth

# View differences before deployment
npm run diff

# Deploy specific stack
npx cdk deploy rapid-photo-upload-dev-network --context environment=dev

# Deploy with approval for IAM/security changes
npx cdk deploy --all --require-approval broadening
```

### First-Time Deployment Order

The stacks have dependencies and will be deployed in the correct order automatically:

1. NetworkStack (foundational)
2. StorageStack & EventStack (parallel, independent)
3. DatabaseStack (depends on NetworkStack)
4. ComputeStack (depends on all previous)
5. MonitoringStack (depends on ComputeStack & DatabaseStack)

## Building and Deploying the Application

After infrastructure is deployed, you need to build and push the Docker image:

### 1. Build the Docker Image

```bash
cd ../backend

# Build the image
docker build -t rapid-photo-upload:latest .
```

### 2. Push to ECR

```bash
# Get ECR repository URI from CDK outputs
export ECR_URI=$(aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`RepositoryUri`].OutputValue' \
  --output text)

# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $ECR_URI

# Tag and push
docker tag rapid-photo-upload:latest $ECR_URI:latest
docker push $ECR_URI:latest
```

### 3. Update ECS Service

The ECS service will automatically pull the new image and perform a rolling deployment:

```bash
# Force new deployment
aws ecs update-service \
  --cluster rapid-photo-upload-dev-cluster \
  --service rapid-photo-upload-dev-service \
  --force-new-deployment
```

## Accessing the Application

After deployment, get the Application Load Balancer URL:

```bash
# Get ALB DNS name
aws cloudformation describe-stacks \
  --stack-name rapid-photo-upload-dev-compute \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDnsName`].OutputValue' \
  --output text
```

Visit `http://<alb-dns-name>` in your browser.

## Environment Variables

The ECS tasks are configured with the following environment variables (automatically set by CDK):

- `SPRING_PROFILES_ACTIVE` - Spring Boot profile (dev/prod)
- `AWS_REGION` - AWS region
- `S3_BUCKET_NAME` - S3 bucket for photo storage
- `EVENTBRIDGE_BUS_NAME` - EventBridge bus name
- `DATABASE_URL` - JDBC connection string
- `DATABASE_USERNAME` - From Secrets Manager
- `DATABASE_PASSWORD` - From Secrets Manager
- `JWT_SECRET` - From Secrets Manager

## Monitoring

### CloudWatch Dashboard

Access the CloudWatch dashboard:
```bash
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=rapid-photo-upload-dev
```

The dashboard includes:
- ECS CPU and memory utilization
- Running task count
- Request count and response time
- HTTP error rates
- Database CPU, connections, and storage

### Alarms

CloudWatch alarms are configured for:
- High ECS CPU/Memory (>85%)
- High response time (>1s)
- HTTP 5xx errors (>10 in 5 minutes)
- Unhealthy targets
- High database CPU (>80%)
- High database connections (>400)
- Low database storage (<5GB)

Alarms send notifications to an SNS topic. To receive email notifications:

```bash
aws sns subscribe \
  --topic-arn <alarm-topic-arn> \
  --protocol email \
  --notification-endpoint your-email@example.com
```

### Logs

View application logs:
```bash
# Via AWS CLI
aws logs tail /ecs/rapid-photo-upload-dev --follow

# Via CloudWatch Console
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Fecs$252Frapid-photo-upload-dev
```

## Auto-Scaling

The ECS service auto-scales based on:
- **CPU Utilization:** Target 70%, scales when sustained above threshold
- **Memory Utilization:** Target 80%
- **Request Count:** Target 1000 requests per task

Scaling configuration:
- **Dev:** Min 1, Max 4 tasks
- **Prod:** Min 2, Max 10 tasks

## Database Management

### Connect to RDS

For database operations, create a bastion host or use ECS Exec:

```bash
# Enable ECS Exec (already enabled for dev)
aws ecs execute-command \
  --cluster rapid-photo-upload-dev-cluster \
  --task <task-id> \
  --container rapid-photo-upload-dev \
  --interactive \
  --command "/bin/sh"

# Then inside the container:
apt-get update && apt-get install -y postgresql-client
psql -h <db-endpoint> -U dbadmin -d photoupload
```

### Database Credentials

Credentials are stored in AWS Secrets Manager:

```bash
# Get database credentials
aws secretsmanager get-secret-value \
  --secret-id rapid-photo-upload-dev-db-credentials \
  --query SecretString \
  --output text | jq .
```

## Cost Optimization

### Development Environment
Estimated monthly cost: **$50-100**
- RDS t4g.micro: ~$15
- ECS Fargate (1 task): ~$25
- NAT Gateway: ~$30
- ALB: ~$20
- Data transfer & storage: ~$10

### Production Environment
Estimated monthly cost: **$400-600**
- RDS r6g.large Multi-AZ: ~$200
- ECS Fargate (2-10 tasks): ~$100-300
- NAT Gateways (3): ~$90
- ALB: ~$20
- Data transfer & storage: variable

### Cost Reduction Tips
1. **Stop dev environment when not in use:**
   ```bash
   # Stop ECS service
   aws ecs update-service --cluster rapid-photo-upload-dev-cluster \
     --service rapid-photo-upload-dev-service --desired-count 0

   # Stop database
   aws rds stop-db-instance --db-instance-identifier rapid-photo-upload-dev-db
   ```

2. **Use Savings Plans or Reserved Instances** for production

3. **Enable S3 Intelligent-Tiering** (already configured for prod)

## Security

### IAM Roles
- **ECS Task Execution Role:** Pull images from ECR, write logs to CloudWatch, read secrets
- **ECS Task Role:** Access S3 bucket (multipart upload), publish to EventBridge

### Security Groups
- **ALB:** Allows HTTP (80) and HTTPS (443) from internet
- **ECS Tasks:** Allows traffic from ALB only on port 8080
- **RDS:** Allows PostgreSQL (5432) from ECS tasks only

### Secrets Management
- Database credentials: AWS Secrets Manager
- JWT secret: AWS Secrets Manager
- No hardcoded secrets in code or environment variables

### Network Security
- Private subnets for ECS tasks and database
- NAT Gateways for outbound internet access
- VPC Flow Logs enabled for monitoring
- Database in isolated subnets (no internet access)

## Troubleshooting

### ECS Tasks Not Starting

1. **Check task logs:**
   ```bash
   aws logs tail /ecs/rapid-photo-upload-dev --follow
   ```

2. **Check task stopped reason:**
   ```bash
   aws ecs describe-tasks \
     --cluster rapid-photo-upload-dev-cluster \
     --tasks <task-id>
   ```

3. **Common issues:**
   - ECR image not found: Push Docker image to ECR
   - Secrets access denied: Check task execution role permissions
   - Database connection timeout: Check security groups and database status

### High Response Times

1. Check ECS CPU/Memory metrics in CloudWatch
2. Review application logs for slow queries
3. Check database performance metrics
4. Consider increasing ECS task size or count

### Database Connection Issues

1. Verify database is running:
   ```bash
   aws rds describe-db-instances --db-instance-identifier rapid-photo-upload-dev-db
   ```

2. Check security groups allow traffic from ECS tasks

3. Verify connection string in ECS task environment variables

## Updating Infrastructure

### Modify Configuration

1. Edit `lib/config.ts` to change resource configurations
2. Review changes:
   ```bash
   npm run diff
   ```
3. Deploy changes:
   ```bash
   npm run deploy:dev
   ```

### Update Application Code

1. Build new Docker image
2. Push to ECR with new tag or `:latest`
3. Force new ECS deployment (see "Update ECS Service" above)

## Cleanup / Destruction

**WARNING:** This will destroy ALL resources and data!

```bash
# Destroy all stacks
npm run destroy

# Or destroy specific environment
npx cdk destroy --all --context environment=dev

# Confirm each stack deletion when prompted
```

**Note:** Some resources may require manual deletion:
- S3 buckets with objects (if `autoDeleteObjects` is false)
- ECR repositories with images
- CloudWatch log groups

## CI/CD Integration

### GitHub Actions Example

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to AWS

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Build Docker image
        run: |
          cd backend
          docker build -t rapid-photo-upload:${{ github.sha }} .

      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI
          docker tag rapid-photo-upload:${{ github.sha }} $ECR_URI:${{ github.sha }}
          docker tag rapid-photo-upload:${{ github.sha }} $ECR_URI:latest
          docker push $ECR_URI:${{ github.sha }}
          docker push $ECR_URI:latest

      - name: Deploy ECS
        run: |
          aws ecs update-service \
            --cluster rapid-photo-upload-prod-cluster \
            --service rapid-photo-upload-prod-service \
            --force-new-deployment
```

## Support

For issues or questions:
1. Check CloudWatch logs for application errors
2. Review CloudWatch alarms for infrastructure issues
3. Consult AWS documentation for service-specific troubleshooting

## Additional Resources

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/)
- [RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)
- [Spring Boot on AWS](https://spring.io/guides/gs/spring-boot-docker/)
