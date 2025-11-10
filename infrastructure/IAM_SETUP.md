# IAM Setup for CDK Deployment

## Overview

This document explains how to set up the IAM permissions required to deploy the RapidPhotoUpload infrastructure using AWS CDK.

## Current Situation

The AWS user `handwriting-math` (ARN: `arn:aws:iam::971422717446:user/handwriting-math`) currently has limited permissions and cannot deploy CDK infrastructure.

## Solution: Custom IAM Policy

We've created a least-privilege IAM policy that grants only the permissions needed for CDK deployments.

### Files Created

1. **cdk-deployment-policy.json** - The IAM policy document with all required permissions
2. **apply-iam-policy.sh** - Automated script to apply the policy (requires administrator access)

## Policy Permissions Breakdown

The policy grants permissions for:

### Core CDK Services
- **CloudFormation** - Deploy and manage infrastructure stacks
- **SSM Parameter Store** - Store CDK bootstrap parameters
- **IAM** - Create roles and policies for AWS services (scoped to RapidPhotoUpload resources)

### Networking (NetworkStack)
- **EC2** - Create VPC, subnets, internet gateways, NAT gateways, route tables, security groups

### Database (DatabaseStack)
- **RDS** - Create PostgreSQL database instances and subnet groups
- **Secrets Manager** - Store database credentials securely

### Storage (StorageStack)
- **S3** - Create and configure photo upload buckets (scoped to rapid-photo-upload-* and cdk-* buckets)

### Events (EventStack)
- **EventBridge** - Create event buses and rules for domain events

### Compute (ComputeStack)
- **ECS** - Create clusters, services, and task definitions
- **ECR** - Create container registries and push images
- **Elastic Load Balancing** - Create application load balancers and target groups
- **Application Auto Scaling** - Configure auto-scaling policies

### Monitoring (MonitoringStack)
- **CloudWatch** - Create dashboards, alarms, and log groups
- **SNS** - Create topics for alarm notifications

## Setup Methods

### Method 1: Automated (Recommended)

Run the provided script as an AWS administrator:

```bash
cd infrastructure
./apply-iam-policy.sh
```

This script will:
1. Create the IAM policy named `RapidPhotoUploadCDKDeployment`
2. Attach it to the `handwriting-math` user
3. Verify the attachment

**Requirements:**
- Must be run by a user with IAM administrator permissions
- AWS CLI must be configured with admin credentials

### Method 2: Manual via AWS Console

1. **Create the Policy:**
   - Go to IAM → Policies → Create Policy
   - Click JSON tab
   - Copy the contents of `cdk-deployment-policy.json`
   - Name it: `RapidPhotoUploadCDKDeployment`
   - Create policy

2. **Attach to User:**
   - Go to IAM → Users → handwriting-math
   - Click "Add permissions" → "Attach policies directly"
   - Search for `RapidPhotoUploadCDKDeployment`
   - Attach policy

### Method 3: Manual via AWS CLI

```bash
# Create the policy
aws iam create-policy \
  --policy-name RapidPhotoUploadCDKDeployment \
  --policy-document file://cdk-deployment-policy.json \
  --description "Permissions for deploying RapidPhotoUpload CDK infrastructure"

# Attach to user
aws iam attach-user-policy \
  --user-name handwriting-math \
  --policy-arn arn:aws:iam::971422717446:policy/RapidPhotoUploadCDKDeployment

# Verify
aws iam list-attached-user-policies --user-name handwriting-math
```

## Security Considerations

### What This Policy Allows

✅ Deploy RapidPhotoUpload infrastructure via CDK
✅ Create resources scoped to this project (where possible)
✅ Manage CloudFormation stacks
✅ Create IAM roles for AWS services (limited to RapidPhotoUpload* and cdk-* resources)

### What This Policy Does NOT Allow

❌ Modify other AWS users or IAM policies
❌ Access resources outside the RapidPhotoUpload project (where scoped)
❌ Delete or modify resources not created by CDK
❌ Perform actions outside the specified services

### Resource Scoping

Where possible, permissions are scoped to specific resources:

- **IAM roles**: `RapidPhotoUpload*` and `cdk-*`
- **S3 buckets**: `rapid-photo-upload-*` and `cdk-*`
- **IAM policies**: `RapidPhotoUpload*`

Some AWS services (like EC2, CloudWatch) don't support resource-level permissions for certain actions, so these use `"Resource": "*"` but are still limited to the specified actions.

## Verification

After applying the policy, verify it's attached:

```bash
aws iam list-attached-user-policies --user-name handwriting-math
```

Expected output:
```json
{
    "AttachedPolicies": [
        {
            "PolicyName": "RapidPhotoUploadCDKDeployment",
            "PolicyArn": "arn:aws:iam::971422717446:policy/RapidPhotoUploadCDKDeployment"
        }
    ]
}
```

## Post-Setup Next Steps

Once the policy is attached:

1. **Wait for IAM propagation** (~60 seconds)
2. **Bootstrap CDK:**
   ```bash
   cd infrastructure
   npx cdk bootstrap
   ```
3. **Deploy infrastructure:**
   ```bash
   npx cdk deploy --all --context environment=dev
   ```
   Or use the automated script:
   ```bash
   ./scripts/deploy.sh dev
   ```

## Troubleshooting

### "Access Denied" errors persist after applying policy

**Solution**: Wait 60-120 seconds for IAM changes to propagate globally.

### Cannot create IAM roles during deployment

**Symptom**: Error creating roles like `RapidPhotoUploadECSTaskRole`

**Solution**: Verify the IAM permissions section includes `iam:CreateRole` for resources matching `RapidPhotoUpload*` and `cdk-*`.

### Cannot create S3 buckets

**Symptom**: Access denied creating S3 bucket

**Solution**: Check that S3 bucket names start with `rapid-photo-upload-` or `cdk-`. The policy scopes S3 permissions to these prefixes.

### Policy size too large

**Symptom**: Error that policy document is too large

**Solution**: The current policy is ~7KB, well under the 6KB limit for managed policies. If customizing, ensure you stay under this limit.

## Updating the Policy

If you need to add permissions later:

```bash
# Create new policy version
aws iam create-policy-version \
  --policy-arn arn:aws:iam::971422717446:policy/RapidPhotoUploadCDKDeployment \
  --policy-document file://cdk-deployment-policy.json \
  --set-as-default

# Delete old version (optional, keep only 5 versions max)
aws iam delete-policy-version \
  --policy-arn arn:aws:iam::971422717446:policy/RapidPhotoUploadCDKDeployment \
  --version-id v1
```

## Cleanup

To remove the policy when no longer needed:

```bash
# Detach from user
aws iam detach-user-policy \
  --user-name handwriting-math \
  --policy-arn arn:aws:iam::971422717446:policy/RapidPhotoUploadCDKDeployment

# Delete policy
aws iam delete-policy \
  --policy-arn arn:aws:iam::971422717446:policy/RapidPhotoUploadCDKDeployment
```

## Alternative: Administrator Access

If you prefer broader permissions for development:

```bash
aws iam attach-user-policy \
  --user-name handwriting-math \
  --policy-arn arn:aws:iam::aws:policy/AdministratorAccess
```

⚠️ **Warning**: This grants full access to all AWS services. Only use for trusted development environments.

## Support

If you encounter permission issues not covered here:

1. Check CloudFormation stack events for specific permission errors
2. Review CloudTrail logs to see which API calls are failing
3. Add the specific missing permissions to `cdk-deployment-policy.json`
4. Update the policy version as shown above
