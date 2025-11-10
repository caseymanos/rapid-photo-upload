# Local Setup Guide - Complete Prerequisites

This guide covers everything you need to set up and run the RapidPhotoUpload system locally.

## Prerequisites Checklist

- [ ] Java 21 installed
- [ ] Maven 3.9+ installed
- [ ] Node.js 18+ installed
- [ ] Docker installed
- [ ] AWS Account with S3 access
- [ ] AWS CLI installed (optional but recommended)

---

## 1. Install Required Software

### Java 21

**Check if installed:**
```bash
java -version
```

**Install on macOS:**
```bash
brew install openjdk@21

# Set JAVA_HOME (add to ~/.zshrc or ~/.bash_profile)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

**Verify:**
```bash
java -version
# Should show: openjdk version "21.x.x"
```

### Maven

**Check if installed:**
```bash
mvn -version
```

**Install on macOS:**
```bash
brew install maven
```

### Node.js 18+

**Check if installed:**
```bash
node -v
npm -v
```

**Install on macOS:**
```bash
brew install node@20
```

### Docker Desktop

**Download and install:**
- Visit: https://www.docker.com/products/docker-desktop/
- Install Docker Desktop for your OS
- Start Docker Desktop

**Verify:**
```bash
docker --version
docker ps
```

---

## 2. AWS Setup

### 2.1 Get AWS Credentials

You need an AWS account with programmatic access.

**Option A: Use Existing AWS Account**

1. Log in to AWS Console: https://console.aws.amazon.com/
2. Go to IAM (Identity and Access Management)
3. Create a new user or use existing user
4. Create Access Key:
   - Click on your user
   - Go to "Security credentials" tab
   - Click "Create access key"
   - Choose "Application running outside AWS"
   - Download credentials (save these securely!)

**Option B: Create New AWS Account**

1. Go to https://aws.amazon.com/
2. Click "Create an AWS Account"
3. Follow setup process
4. Then follow Option A to create access keys

**Important**: Save these credentials:
- `AWS_ACCESS_KEY_ID` (e.g., AKIAIOSFODNN7EXAMPLE)
- `AWS_SECRET_ACCESS_KEY` (e.g., wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY)

### 2.2 Configure AWS CLI (Optional but Recommended)

**Install AWS CLI:**
```bash
# macOS
brew install awscli

# Verify
aws --version
```

**Configure credentials:**
```bash
aws configure
```

Enter when prompted:
- **AWS Access Key ID**: Your access key
- **AWS Secret Access Key**: Your secret key
- **Default region name**: `us-east-1` (or your preferred region)
- **Default output format**: `json`

This creates `~/.aws/credentials` and `~/.aws/config` files.

**Verify:**
```bash
aws s3 ls
# Should list your S3 buckets (if you have any)
```

### 2.3 Create S3 Bucket

You need an S3 bucket to store uploaded photos.

**Option A: Create via AWS Console**

1. Go to S3: https://s3.console.aws.amazon.com/
2. Click "Create bucket"
3. **Bucket name**: `rapid-photo-upload-dev-YOUR-NAME` (must be globally unique)
4. **Region**: `us-east-1` (or your preferred region)
5. **Block Public Access**: Keep all 4 checkboxes CHECKED (recommended for security)
6. **Bucket Versioning**: Disabled (or enable if you want)
7. Click "Create bucket"

**Option B: Create via AWS CLI**

```bash
# Replace with your unique bucket name
BUCKET_NAME="rapid-photo-upload-dev-$(whoami)"
AWS_REGION="us-east-1"

# Create bucket
aws s3 mb s3://${BUCKET_NAME} --region ${AWS_REGION}

# Verify
aws s3 ls | grep rapid-photo-upload
```

**Configure CORS for S3 Bucket (Required for direct uploads):**

Create a file `cors-config.json`:
```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedOrigins": ["http://localhost:3000", "http://localhost:8080"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

Apply CORS configuration:
```bash
aws s3api put-bucket-cors --bucket ${BUCKET_NAME} --cors-configuration file://cors-config.json
```

**Set your bucket name** (save this for later):
```bash
echo "Your S3 bucket name: ${BUCKET_NAME}"
# Example: rapid-photo-upload-dev-caseymanos
```

---

## 3. Backend Configuration

### 3.1 Create `.env.local` File

Navigate to backend directory:
```bash
cd backend
```

Copy the example file:
```bash
cp .env.local.example .env.local
```

### 3.2 Edit `.env.local`

Open `.env.local` and update with your values:

```bash
# Database Configuration (leave as-is for local Docker PostgreSQL)
DATABASE_URL=jdbc:postgresql://localhost:5432/photoupload
DATABASE_USERNAME=dbadmin
DATABASE_PASSWORD=password

# AWS Configuration (REPLACE WITH YOUR VALUES)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY_HERE
AWS_SECRET_ACCESS_KEY=YOUR_SECRET_KEY_HERE

# S3 Bucket Configuration (REPLACE WITH YOUR BUCKET NAME)
S3_BUCKET_NAME=rapid-photo-upload-dev-YOUR-NAME

# EventBridge Configuration (optional, can leave as default)
EVENTBRIDGE_BUS_NAME=default

# JWT Secret (GENERATE A NEW ONE)
# Generate with: openssl rand -base64 32
JWT_SECRET=your-generated-jwt-secret-here

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
```

### 3.3 Generate JWT Secret

```bash
openssl rand -base64 32
```

Copy the output and paste it as your `JWT_SECRET` value.

**Example .env.local (with fake values):**
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/photoupload
DATABASE_USERNAME=dbadmin
DATABASE_PASSWORD=password

AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

S3_BUCKET_NAME=rapid-photo-upload-dev-caseymanos

EVENTBRIDGE_BUS_NAME=default

JWT_SECRET=dGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JKV1RUb2tlbnM=

SPRING_PROFILES_ACTIVE=dev
```

---

## 4. Database Setup

### 4.1 Start PostgreSQL with Docker

```bash
docker run --name postgres-photoupload \
  -e POSTGRES_DB=photoupload \
  -e POSTGRES_USER=dbadmin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

**Verify it's running:**
```bash
docker ps | grep postgres-photoupload
```

**Check logs if there are issues:**
```bash
docker logs postgres-photoupload
```

### 4.2 Test Database Connection

```bash
# Connect to database
docker exec -it postgres-photoupload psql -U dbadmin -d photoupload

# You should see:
# photoupload=#

# List tables (will be empty initially)
\dt

# Exit
\q
```

---

## 5. Backend Build and Run

### 5.1 Install Dependencies and Build

```bash
cd backend

# Clean and build
mvn clean install
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

### 5.2 Run Backend

**Option A: Use run script**
```bash
./run.sh
```

**Option B: Use Maven directly**
```bash
JAVA_HOME=/path/to/java-21 mvn spring-boot:run
```

**Expected output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::

...
Started RapidPhotoUploadApplication in X.XXX seconds
```

**Verify backend is running:**
```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

---

## 6. Frontend Setup

### 6.1 Install Dependencies

Open a **new terminal** (keep backend running):

```bash
cd frontend-web

# Install dependencies
npm install
```

### 6.2 Configure Frontend (Optional)

The frontend uses environment variables. Create `.env` if needed:

```bash
# frontend-web/.env
VITE_API_URL=http://localhost:8080
```

(This is already set by default via Vite proxy)

### 6.3 Run Frontend

```bash
npm run dev
```

**Expected output:**
```
  VITE v5.x.x  ready in XXX ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

**Verify frontend is running:**

Open browser: http://localhost:3000

---

## 7. Verify Complete Setup

### 7.1 System Health Check

**Backend:**
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

**Frontend:**
```bash
curl http://localhost:3000
# Should return HTML
```

**Database:**
```bash
docker exec -it postgres-photoupload psql -U dbadmin -d photoupload -c "\dt"
```

**AWS S3:**
```bash
aws s3 ls s3://your-bucket-name/
# Should work without errors
```

### 7.2 Test Complete Flow

1. **Open browser**: http://localhost:3000
2. **Register account**:
   - Click "create a new account"
   - Email: `test@example.com`
   - Password: `password123`
   - Click "Create account"
3. **Upload photo**:
   - Select a test image
   - Watch progress bar
   - Should complete successfully
4. **View in gallery**:
   - Click "View Gallery"
   - Photo should appear
5. **Check S3**:
   ```bash
   aws s3 ls s3://your-bucket-name/uploads/ --recursive
   ```
   You should see your uploaded photo!

---

## 8. Troubleshooting

### Backend Won't Start

**Issue: Port 8080 already in use**
```bash
lsof -ti:8080 | xargs kill -9
```

**Issue: Database connection failed**
```bash
# Check PostgreSQL is running
docker ps | grep postgres-photoupload

# Restart if needed
docker restart postgres-photoupload
```

**Issue: AWS credentials invalid**
- Verify credentials in `.env.local`
- Test with: `aws s3 ls`
- Check IAM permissions (need S3 read/write)

### Frontend Won't Start

**Issue: Port 3000 already in use**
```bash
lsof -ti:3000 | xargs kill -9
```

**Issue: Dependencies not installed**
```bash
cd frontend-web
rm -rf node_modules package-lock.json
npm install
```

### Upload Fails

**Issue: S3 access denied**
- Check bucket name matches `.env.local`
- Verify CORS configuration on bucket
- Check AWS credentials have S3 permissions

**Issue: Presigned URL expired**
- URLs expire after 2 hours
- Restart backend to generate fresh URLs

**Issue: CORS error in browser**
```bash
# Re-apply CORS configuration
aws s3api put-bucket-cors --bucket your-bucket-name --cors-configuration file://cors-config.json
```

---

## 9. Environment Variables Reference

### Backend (.env.local)

| Variable | Example | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/photoupload` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | `dbadmin` | Database username |
| `DATABASE_PASSWORD` | `password` | Database password |
| `AWS_REGION` | `us-east-1` | AWS region |
| `AWS_ACCESS_KEY_ID` | `AKIAIOSFODNN7EXAMPLE` | AWS access key |
| `AWS_SECRET_ACCESS_KEY` | `wJalrXUtnFEMI/...` | AWS secret key |
| `S3_BUCKET_NAME` | `rapid-photo-upload-dev-username` | S3 bucket name |
| `JWT_SECRET` | `base64-encoded-string` | JWT signing secret |

### Frontend (.env)

| Variable | Example | Description |
|----------|---------|-------------|
| `VITE_API_URL` | `http://localhost:8080` | Backend API URL |

---

## 10. AWS Permissions Required

Your AWS IAM user needs these permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:s3:::rapid-photo-upload-dev-*",
        "arn:aws:s3:::rapid-photo-upload-dev-*/*"
      ]
    }
  ]
}
```

---

## 11. Quick Start Commands (After Initial Setup)

Once everything is configured, use these to start the system:

```bash
# Terminal 1: Start PostgreSQL (if not running)
docker start postgres-photoupload

# Terminal 2: Start Backend
cd backend && ./run.sh

# Terminal 3: Start Frontend
cd frontend-web && npm run dev

# Browser: Open http://localhost:3000
```

---

## 12. Stopping the System

```bash
# Stop Frontend (Ctrl+C in Terminal 3)

# Stop Backend (Ctrl+C in Terminal 2)

# Stop PostgreSQL
docker stop postgres-photoupload

# (Optional) Remove PostgreSQL container
docker rm postgres-photoupload
```

---

## Summary

You now have everything configured:

✅ Java 21 installed
✅ Maven installed
✅ Node.js installed
✅ Docker running PostgreSQL
✅ AWS credentials configured
✅ S3 bucket created with CORS
✅ Backend `.env.local` configured
✅ Frontend dependencies installed
✅ System ready to run!

**Next**: Follow the quick start commands above or see `RUN_COMPLETE_SYSTEM.md` for detailed startup instructions.
