#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { NetworkStack } from '../lib/network-stack';
import { DatabaseStack } from '../lib/database-stack';
import { StorageStack } from '../lib/storage-stack';
import { EventStack } from '../lib/event-stack';
import { ComputeStack } from '../lib/compute-stack';
import { MonitoringStack } from '../lib/monitoring-stack';
import { FrontendStack } from '../lib/frontend-stack';
import { getConfig } from '../lib/config';

const app = new cdk.App();

// Get environment from context (default to 'dev')
const environment = (app.node.tryGetContext('environment') || 'dev') as 'dev' | 'prod';

// Get AWS account and region from environment or use defaults
const account = process.env.CDK_DEFAULT_ACCOUNT;
const region = process.env.CDK_DEFAULT_REGION || 'us-east-1';

if (!account) {
  throw new Error(
    'CDK_DEFAULT_ACCOUNT environment variable is not set. ' +
    'Please configure AWS credentials or run: export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)'
  );
}

// Get configuration for the environment
const config = getConfig(environment, account, region);

// Define stack environment
const env = {
  account,
  region: config.region,
};

// Create stacks with proper dependencies

// 1. Network Stack (foundational)
const networkStack = new NetworkStack(app, `${config.appName}-${environment}-network`, {
  config,
  env,
  description: `Network infrastructure for ${config.appName} ${environment}`,
});

// 2. Storage Stack (independent)
const storageStack = new StorageStack(app, `${config.appName}-${environment}-storage`, {
  config,
  env,
  description: `S3 storage infrastructure for ${config.appName} ${environment}`,
});

// 3. Event Stack (independent)
const eventStack = new EventStack(app, `${config.appName}-${environment}-event`, {
  config,
  env,
  description: `EventBridge infrastructure for ${config.appName} ${environment}`,
});

// 4. Database Stack (depends on Network)
const databaseStack = new DatabaseStack(app, `${config.appName}-${environment}-database`, {
  config,
  vpc: networkStack.vpc,
  databaseSecurityGroup: networkStack.databaseSecurityGroup,
  env,
  description: `RDS PostgreSQL infrastructure for ${config.appName} ${environment}`,
});
databaseStack.addDependency(networkStack);

// 5. Compute Stack (depends on Network, Database, Storage, and Event)
const computeStack = new ComputeStack(app, `${config.appName}-${environment}-compute`, {
  config,
  vpc: networkStack.vpc,
  albSecurityGroup: networkStack.albSecurityGroup,
  ecsSecurityGroup: networkStack.ecsSecurityGroup,
  databaseUrl: databaseStack.databaseUrl,
  databaseSecret: databaseStack.databaseSecret,
  photoBucketName: storageStack.photoBucket.bucketName,
  eventBusName: eventStack.eventBusName,
  env,
  description: `ECS Fargate compute infrastructure for ${config.appName} ${environment}`,
});
computeStack.addDependency(networkStack);
computeStack.addDependency(databaseStack);
computeStack.addDependency(storageStack);
computeStack.addDependency(eventStack);

// 6. Monitoring Stack (depends on Compute and Database)
const monitoringStack = new MonitoringStack(app, `${config.appName}-${environment}-monitoring`, {
  config,
  cluster: computeStack.cluster,
  service: computeStack.service,
  loadBalancer: computeStack.loadBalancer,
  database: databaseStack.database,
  env,
  description: `CloudWatch monitoring for ${config.appName} ${environment}`,
});
monitoringStack.addDependency(computeStack);
monitoringStack.addDependency(databaseStack);

// 7. Frontend Stack (independent - CloudFront + S3)
const frontendStack = new FrontendStack(app, `${config.appName}-${environment}-frontend`, {
  config,
  photoBucket: storageStack.photoBucket,
  env,
  description: `CloudFront and S3 infrastructure for ${config.appName} ${environment} web app`,
});
frontendStack.addDependency(storageStack);

// Add global tags
cdk.Tags.of(app).add('Project', config.appName);
cdk.Tags.of(app).add('Environment', environment);
cdk.Tags.of(app).add('ManagedBy', 'CDK');
