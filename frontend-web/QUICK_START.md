# Quick Start Guide

## Prerequisites

- Node.js 18+ installed
- Backend API running on `http://localhost:8080`

## Setup

### 1. Install Dependencies

```bash
cd frontend-web
npm install
```

Or use the setup script:

```bash
chmod +x setup.sh
./setup.sh
```

### 2. Start Development Server

```bash
npm run dev
```

The application will start at `http://localhost:3000`

## First Time Usage

### 1. Create an Account

1. Navigate to `http://localhost:3000`
2. You'll be redirected to the login page
3. Click "create a new account"
4. Enter your email and password (min 6 characters)
5. Click "Create account"

### 2. Upload Photos

1. After login, you'll see the Upload page
2. Drag and drop photos or click to select files
3. Upload up to 100 photos simultaneously
4. Watch real-time progress for each upload
5. Supported formats: JPEG, PNG, GIF, WebP (max 50MB each)

### 3. View Gallery

1. Click "View Gallery" or navigate to the Gallery tab
2. See all your uploaded photos in a grid
3. Click on any photo to view details
4. Add tags and descriptions
5. Save changes

## Testing the System

### Test 100 Concurrent Uploads

1. Prepare 100 test images (2MB each recommended)
2. Go to the Upload page
3. Select all 100 files at once
4. Observe:
   - All files queued immediately
   - 10 uploads running concurrently
   - Real-time progress for each file
   - UI remains responsive
   - All uploads complete successfully

### Expected Performance

- **Concurrent uploads**: 10 at a time (configurable)
- **Upload speed**: Limited by your internet connection
- **UI responsiveness**: Smooth during all operations
- **Progress updates**: Real-time for each file

## Troubleshooting

### Backend Connection Issues

If you see connection errors:

1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check Vite proxy configuration in `vite.config.ts`
3. Ensure no CORS issues (backend should allow localhost:3000)

### Upload Failures

If uploads fail:

1. Check file size (max 50MB)
2. Verify file type (images only)
3. Check AWS S3 credentials in backend
4. Look at browser console for errors

### Authentication Issues

If login doesn't work:

1. Clear browser localStorage
2. Verify backend auth endpoints are working
3. Check JWT token in browser dev tools (Application → Local Storage)

## Development

### Project Structure

```
src/
├── features/
│   ├── auth/              # Authentication
│   ├── upload/            # Upload feature
│   └── gallery/           # Photo gallery
├── shared/
│   ├── api/              # API client
│   ├── types/            # TypeScript types
│   └── hooks/            # Shared hooks
├── components/           # Shared components
└── App.tsx              # Main app
```

### Key Files

- `src/features/upload/services/uploadService.ts` - S3 multipart upload logic
- `src/features/upload/hooks/useUploadManager.ts` - Concurrency control
- `src/shared/api/apiClient.ts` - Axios client with JWT
- `src/shared/api/endpoints.ts` - API endpoints

## Build for Production

```bash
npm run build
```

Output will be in `dist/` directory.

Preview production build:

```bash
npm run preview
```

## Next Steps

- Configure AWS CloudFront URL for photo serving
- Add environment-specific configurations
- Set up CI/CD pipeline
- Add error tracking (e.g., Sentry)
- Implement caching strategies
