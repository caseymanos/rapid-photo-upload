// API Response Types
export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
}

export interface PresignedUrl {
  partNumber: number;
  url: string;
}

export interface InitiateUploadResponse {
  photoId: string;
  s3Key: string;
  multipartUploadId: string;
  presignedUrls: PresignedUrl[];
  expiresAt?: string;
  singlePartUpload?: boolean;
  chunkSizeBytes?: number;
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
  thumbnailVariants?: Record<string, string>;
  thumbnailFallbackUrl?: string;
  placeholderUrl?: string;
  placeholderFallbackUrl?: string;
  placeholderBase64?: string;
  downloadUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedPhotosResponse {
  items: PhotoResponse[];
  page: number;
  size: number;
  totalElements: number;
  hasNext: boolean;
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
  totalBytesUploaded?: number;
  avgUploadDurationMs?: number;
  avgThroughputMbps?: number;
  minUploadDurationMs?: number;
  maxUploadDurationMs?: number;
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

export interface CreateSessionRequest {
  expectedPhotoCount: number;
}

export interface InitiateUploadRequest {
  originalFilename: string;
  fileSizeBytes: number;
  mimeType: string;
  uploadSessionId?: string;
}

export interface CompletedPart {
  partNumber: number;
  etag: string;
}

export interface CompleteUploadRequest {
  parts: CompletedPart[];
}

export interface UpdatePhotoMetadataRequest {
  tags?: string[];
  metadata?: PhotoMetadata;
}

export interface UpdateSessionMetricsRequest {
  totalBytesUploaded: number;
  avgUploadDurationMs: number;
  avgThroughputMbps: number;
  minUploadDurationMs: number;
  maxUploadDurationMs: number;
}

// Frontend-specific types
export interface UploadItem {
  id: string;
  file: File;
  photoId?: string;
  uploadId?: string;
  status: 'pending' | 'uploading' | 'completed' | 'failed';
  progress: number;
  error?: string;
  startTime?: number;
  endTime?: number;
  fileSize?: number;
  uploadDurationMs?: number;
}

export interface UploadProgress {
  total: number;
  uploaded: number;
  percentage: number;
}

// Stats types
export interface UploadStatsResponse {
  aggregate: AggregateStats;
  recentSessions: SessionSummary[];
}

export interface AggregateStats {
  totalPhotos: number;
  totalBytesUploaded: number;
  totalSessions: number;
  completedSessions: number;
  failedSessions: number;
  avgUploadDurationMs: number | null;
  avgThroughputMbps: number | null;
  successRate: number;
}

export interface SessionSummary {
  sessionId: string;
  totalPhotos: number;
  completedPhotos: number;
  failedPhotos: number;
  status: string;
  totalBytesUploaded: number | null;
  avgUploadDurationMs: number | null;
  avgThroughputMbps: number | null;
  startedAt: string;
  completedAt: string | null;
}
