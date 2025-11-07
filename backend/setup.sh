#!/bin/bash

# RapidPhotoUpload Backend Setup Script
# This script sets up the local development environment

set -e

echo "🚀 RapidPhotoUpload Backend Setup"
echo "=================================="
echo ""

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check prerequisites
echo "Checking prerequisites..."

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        print_status "Java $JAVA_VERSION installed"
    else
        print_error "Java 21+ required, found version $JAVA_VERSION"
        exit 1
    fi
else
    print_error "Java not found. Please install Java 21+"
    exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
    print_status "Maven installed: $(mvn -version | head -n 1)"
else
    print_warning "Maven not found. Installing via Homebrew..."
    if command -v brew &> /dev/null; then
        brew install maven
        print_status "Maven installed"
    else
        print_error "Homebrew not found. Please install Maven manually: https://maven.apache.org/install.html"
        exit 1
    fi
fi

# Check Docker
if command -v docker &> /dev/null; then
    if docker ps &> /dev/null; then
        print_status "Docker is running"
    else
        print_warning "Docker is installed but not running. Starting Docker..."
        open -a Docker
        echo "Waiting for Docker to start..."
        while ! docker ps &> /dev/null; do
            sleep 2
            printf "."
        done
        echo ""
        print_status "Docker started"
    fi
else
    print_error "Docker not found. Please install Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Check AWS CLI
if command -v aws &> /dev/null; then
    print_status "AWS CLI installed"
    if aws configure list &> /dev/null; then
        print_status "AWS credentials configured"
    else
        print_warning "AWS credentials not configured"
        echo "Please run: aws configure"
    fi
else
    print_warning "AWS CLI not found. Install with: brew install awscli"
fi

echo ""
echo "Setting up PostgreSQL database..."

# Check if container already exists
if docker ps -a --format '{{.Names}}' | grep -q "^postgres-photoupload$"; then
    print_warning "PostgreSQL container already exists"

    # Check if it's running
    if docker ps --format '{{.Names}}' | grep -q "^postgres-photoupload$"; then
        print_status "PostgreSQL container is running"
    else
        echo "Starting existing PostgreSQL container..."
        docker start postgres-photoupload
        print_status "PostgreSQL container started"
    fi
else
    echo "Creating PostgreSQL container..."
    docker run --name postgres-photoupload \
      -e POSTGRES_DB=photoupload \
      -e POSTGRES_USER=dbadmin \
      -e POSTGRES_PASSWORD=password \
      -p 5432:5432 \
      -d postgres:15

    # Wait for PostgreSQL to be ready
    echo "Waiting for PostgreSQL to be ready..."
    sleep 5
    print_status "PostgreSQL container created and started"
fi

echo ""
echo "Checking environment configuration..."

# Check for .env.local file
if [ ! -f ".env.local" ]; then
    print_warning ".env.local not found"
    echo "Please create .env.local from .env.local.example and fill in your values"
    echo "Copy command: cp .env.local.example .env.local"
else
    print_status ".env.local found"
fi

echo ""
echo "Building application..."
mvn clean install -DskipTests

print_status "Build completed"

echo ""
echo "=================================="
echo "✅ Setup Complete!"
echo "=================================="
echo ""
echo "Next steps:"
echo "1. If you haven't already, create and configure .env.local:"
echo "   cp .env.local.example .env.local"
echo "   # Edit .env.local with your actual AWS credentials and S3 bucket"
echo ""
echo "2. Create S3 bucket (if needed):"
echo "   aws s3 mb s3://rapid-photo-upload-dev --region us-east-1"
echo ""
echo "3. Run tests:"
echo "   mvn test"
echo ""
echo "4. Start the application:"
echo "   source .env.local && mvn spring-boot:run"
echo ""
echo "5. Verify the application is running:"
echo "   curl http://localhost:8080/actuator/health"
echo ""
