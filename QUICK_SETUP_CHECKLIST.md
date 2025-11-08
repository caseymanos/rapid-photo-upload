# Quick Setup Checklist

Use this as a quick reference while setting up the system. See `LOCAL_SETUP_GUIDE.md` for detailed instructions.

## 1. Software Installation

```bash
# Check what you have
java -version        # Need: 21+
mvn -version         # Need: 3.9+
node -v              # Need: 18+
docker --version     # Need: any recent version
aws --version        # Optional but recommended

# Install missing (macOS)
brew install openjdk@21 maven node@20 awscli
```

## 2. AWS Setup (Critical!)

### Get AWS Credentials
1. Go to AWS Console: https://console.aws.amazon.com/iam/
2. Your User → Security credentials → Create access key
3. Save these securely:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`

### Configure AWS CLI
```bash
aws configure
# Enter: Access Key ID, Secret Key, Region (us-east-1), Format (json)
```

### Create S3 Bucket (Automated)
```bash
./setup-aws.sh
```

### OR Create S3 Bucket (Manual)
```bash
# Create bucket with unique name
BUCKET_NAME="rapid-photo-upload-dev-$(whoami)"
aws s3 mb s3://${BUCKET_NAME} --region us-east-1

# Apply CORS
aws s3api put-bucket-cors --bucket ${BUCKET_NAME} --cors-configuration file://cors-config.json

# Save bucket name for later!
echo ${BUCKET_NAME}
```

## 3. Backend Configuration

```bash
cd backend

# Copy and edit environment file
cp .env.local.example .env.local

# Edit .env.local with your values:
nano .env.local  # or use your preferred editor
```

**Required values in `.env.local`:**
```bash
AWS_ACCESS_KEY_ID=YOUR_KEY_HERE              # From step 2
AWS_SECRET_ACCESS_KEY=YOUR_SECRET_HERE       # From step 2
S3_BUCKET_NAME=YOUR_BUCKET_NAME              # From step 2
JWT_SECRET=$(openssl rand -base64 32)        # Generate new
```

## 4. Database Setup

```bash
# Start PostgreSQL in Docker
docker run --name postgres-photoupload \
  -e POSTGRES_DB=photoupload \
  -e POSTGRES_USER=dbadmin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15

# Verify
docker ps | grep postgres-photoupload
```

## 5. Install & Run

### Backend
```bash
cd backend
mvn clean install     # First time only
./run.sh

# Verify: curl http://localhost:8080/actuator/health
```

### Frontend
```bash
cd frontend-web
npm install          # First time only
npm run dev

# Verify: Open http://localhost:3000
```

## 6. Test Upload

1. Open http://localhost:3000
2. Register account (email: test@example.com, password: password123)
3. Upload a photo
4. Check it appears in S3:
   ```bash
   aws s3 ls s3://your-bucket-name/uploads/ --recursive
   ```

---

## Essential Environment Variables

### Backend (.env.local)
```bash
# Database (local Docker)
DATABASE_URL=jdbc:postgresql://localhost:5432/photoupload
DATABASE_USERNAME=dbadmin
DATABASE_PASSWORD=password

# AWS (REQUIRED - use your values)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=AKIAXXXXXXXXXXXXXXXX
AWS_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# S3 Bucket (REQUIRED - your unique bucket)
S3_BUCKET_NAME=rapid-photo-upload-dev-YOUR-NAME

# JWT Secret (REQUIRED - generate with: openssl rand -base64 32)
JWT_SECRET=your-generated-secret-here
```

---

## Common Issues

### "AWS credentials not found"
→ Check `.env.local` has correct AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY

### "Access Denied" on upload
→ Check S3 bucket CORS: `aws s3api get-bucket-cors --bucket YOUR_BUCKET`
→ Re-apply: `aws s3api put-bucket-cors --bucket YOUR_BUCKET --cors-configuration file://cors-config.json`

### "Port 8080 already in use"
→ Kill: `lsof -ti:8080 | xargs kill -9`

### "Database connection failed"
→ Check PostgreSQL: `docker ps | grep postgres-photoupload`
→ Restart: `docker restart postgres-photoupload`

---

## Quick Commands

### Start Everything
```bash
# Terminal 1
docker start postgres-photoupload

# Terminal 2
cd backend && ./run.sh

# Terminal 3
cd frontend-web && npm run dev
```

### Stop Everything
```bash
# Ctrl+C in each terminal, then:
docker stop postgres-photoupload
```

### Check Health
```bash
# Backend
curl http://localhost:8080/actuator/health

# Database
docker exec -it postgres-photoupload psql -U dbadmin -d photoupload -c "\dt"

# S3
aws s3 ls s3://YOUR_BUCKET_NAME/
```

---

## Costs

### AWS S3 Pricing (us-east-1)
- Storage: $0.023 per GB/month
- PUT requests: $0.005 per 1,000 requests
- GET requests: $0.0004 per 1,000 requests

**Estimated monthly cost for development:**
- 100 photos × 2MB = 200MB storage → ~$0.01/month
- 100 uploads + views → ~$0.01/month
- **Total: ~$0.02/month** (essentially free)

---

## Next Steps

Once running successfully:
1. Test with 10-20 photos
2. Test with 100 photos simultaneously
3. Review the code in `frontend-web/src/`
4. Check photos in S3 bucket
5. Explore the gallery features

For detailed explanations, see:
- `LOCAL_SETUP_GUIDE.md` - Complete setup instructions
- `RUN_COMPLETE_SYSTEM.md` - Running the system
- `FRONTEND_BACKEND_INTEGRATION.md` - How it all works together
