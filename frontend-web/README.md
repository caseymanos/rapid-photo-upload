# RapidPhotoUpload Frontend

React web application for high-performance photo uploads with real-time progress tracking.

## Features

- **High-Volume Upload**: Upload up to 100 photos simultaneously
- **Direct S3 Upload**: Uses presigned URLs for direct client-to-S3 uploads
- **Real-Time Progress**: Live progress tracking for all uploads
- **Concurrent Control**: Intelligent concurrency management (10 simultaneous uploads)
- **Photo Gallery**: View and manage uploaded photos
- **Metadata Editing**: Add tags and descriptions to photos
- **Responsive UI**: Smooth performance during peak upload operations
- **JWT Authentication**: Secure access control

## Tech Stack

- **React 18** with TypeScript
- **Vite** for fast development and builds
- **TailwindCSS** for styling
- **React Router** for navigation
- **Zustand** for state management
- **Axios** for HTTP requests

## Quick Start

### Prerequisites

- Node.js 18+ and npm
- Backend API running on `http://localhost:8080`

### Installation

```bash
cd frontend-web
npm install
```

### Development

```bash
npm run dev
```

The app will start on `http://localhost:3000` and proxy API requests to the backend.

### Build for Production

```bash
npm run build
npm run preview
```

## Project Structure

```
src/
├── features/
│   ├── auth/
│   │   ├── components/     # Login/Register forms
│   │   └── store/          # Auth state management
│   ├── upload/
│   │   ├── components/     # Upload UI components
│   │   ├── hooks/          # useUploadManager hook
│   │   ├── services/       # Upload service with S3 multipart
│   │   └── pages/          # Upload page
│   └── gallery/
│       ├── components/     # Gallery UI components
│       ├── hooks/          # usePhotos hook
│       └── pages/          # Gallery page
├── shared/
│   ├── api/               # API client and endpoints
│   ├── types/             # TypeScript types
│   └── hooks/             # Shared hooks
├── components/            # Shared components (Layout, ProtectedRoute)
├── App.tsx               # Main app with routing
└── main.tsx              # App entry point
```

## Key Features Explained

### Multipart Upload with Presigned URLs

The upload service implements S3 multipart upload:

1. Client requests upload initiation from backend
2. Backend generates presigned URLs for each 5MB chunk
3. Client uploads chunks directly to S3 in parallel
4. Client notifies backend on completion
5. Backend finalizes the multipart upload

This approach:
- Offloads bandwidth from backend servers
- Enables true parallel uploads (100 concurrent)
- Provides real-time progress tracking
- Reduces server load and costs

### Concurrency Control

The `useUploadManager` hook manages upload concurrency:

- **Queue Management**: Maintains a queue of pending uploads
- **Concurrent Limit**: Processes up to 10 uploads simultaneously
- **Auto-Processing**: Automatically starts next upload when one completes
- **Abort Support**: Can cancel individual uploads
- **Retry Logic**: Failed uploads can be retried
- **Statistics**: Tracks total, completed, failed, uploading, and pending counts

### Authentication

Uses JWT tokens for authentication:

- Tokens stored in localStorage
- Axios interceptor adds token to all requests
- Automatic redirect on 401 responses
- Protected routes require authentication

## API Integration

The frontend integrates with the Spring Boot backend:

### Auth Endpoints

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login

### Upload Endpoints

- `POST /api/v1/uploads/initiate` - Initiate multipart upload
- `POST /api/v1/uploads/{photoId}/complete` - Complete upload
- `GET /api/v1/uploads/sessions/{sessionId}/status` - Get session status

### Photo Endpoints

- `GET /api/v1/photos` - List photos
- `GET /api/v1/photos/{photoId}` - Get photo details
- `PUT /api/v1/photos/{photoId}/metadata` - Update metadata

## Configuration

### API Proxy

The Vite dev server proxies API requests to the backend (configured in `vite.config.ts`):

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

### Environment Variables

Create a `.env` file for custom configuration:

```
VITE_API_URL=http://localhost:8080
```

## Performance Considerations

- **Lazy Loading**: Gallery images load on demand
- **Optimistic UI**: Immediate feedback on user actions
- **Chunked Uploads**: 5MB chunks for optimal S3 performance
- **Concurrent Limits**: Prevents browser/network overload
- **Progress Throttling**: Updates at reasonable intervals

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+

## License

MIT
