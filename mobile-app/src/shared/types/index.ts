// API Response Types
export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
}

export interface PresignedUrl {
  partNumber: number;
  url: string;
  expiration: string;
}

export interface InitiateUploadResponse {
  photoId: string;
  uploadId: string;
  s3Key: string;
  presignedUrls: PresignedUrl[];
}

export interface PhotoResponse {
  id: string;
  userId: string;
  uploadSessionId?: string;
  s3Key: string;
  s3Bucket: string;
  s3Etag?: string;
  originalFilename: string;
  fileSizeBytes: number;
  mimeType: string;
  uploadStatus: UploadStatus;
  tags: string[];
  metadata: PhotoMetadata;
  thumbnailUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PhotoMetadata {
  width?: number;
  height?: number;
  camera?: string;
  location?: string;
  description?: string;
  [key: string]: any;
}

export interface SessionStatusResponse {
  id: string;
  userId: string;
  sessionToken: string;
  totalPhotos: number;
  completedPhotos: number;
  failedPhotos: number;
  status: SessionStatus;
  startedAt: string;
  completedAt?: string;
}

// Enums
export enum UploadStatus {
  INITIATED = 'INITIATED',
  UPLOADING = 'UPLOADING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

export enum SessionStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

// Request Types
export interface RegisterRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface InitiateUploadRequest {
  filename: string;
  fileSizeBytes: number;
  mimeType: string;
  uploadSessionId?: string;
}

export interface CompletedPart {
  partNumber: number;
  etag: string;
}

export interface CompleteUploadRequest {
  uploadId: string;
  parts: CompletedPart[];
}

export interface UpdatePhotoMetadataRequest {
  tags?: string[];
  metadata?: PhotoMetadata;
}

// Mobile-specific types (adapted from web)
export interface UploadItem {
  id: string;
  uri: string; // Mobile uses uri instead of File object
  filename: string;
  fileSize: number;
  mimeType: string;
  photoId?: string;
  uploadId?: string;
  status: 'pending' | 'uploading' | 'completed' | 'failed';
  progress: number;
  error?: string;
  startTime?: number;
  endTime?: number;
}

export interface UploadProgress {
  total: number;
  uploaded: number;
  percentage: number;
}
