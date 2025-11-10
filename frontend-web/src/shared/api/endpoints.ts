import { apiClient } from './apiClient';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  InitiateUploadRequest,
  InitiateUploadResponse,
  CompleteUploadRequest,
  PhotoResponse,
  UpdatePhotoMetadataRequest,
  SessionStatusResponse,
  PaginatedPhotosResponse,
} from '../types';

// Auth endpoints
export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>('/auth/register', data),

  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/auth/login', data),

  logout: () => {
    apiClient.clearToken();
  },
};

// Upload endpoints
export const uploadApi = {
  initiateUpload: (data: InitiateUploadRequest) =>
    apiClient.post<InitiateUploadResponse>('/uploads/initiate', data),

  completeUpload: (photoId: string, data: CompleteUploadRequest) =>
    apiClient.post<void>(`/uploads/${photoId}/complete`, data),

  getSessionStatus: (sessionId: string) =>
    apiClient.get<SessionStatusResponse>(`/uploads/sessions/${sessionId}/status`),
};

// Photo endpoints
type GetPhotosParams = {
  page?: number;
  size?: number;
  includeDownloadUrl?: boolean;
};

export const photoApi = {
  getPhotos: ({ page = 0, size = 30, includeDownloadUrl = false }: GetPhotosParams = {}) =>
    apiClient.get<PaginatedPhotosResponse>('/photos', {
      params: { page, size, includeDownloadUrl },
    }),

  getPhotoById: (photoId: string) =>
    apiClient.get<PhotoResponse>(`/photos/${photoId}`),

  getPhotoDownloadUrl: (photoId: string) =>
    apiClient.get<{ downloadUrl: string }>(`/photos/${photoId}/download-url`),

  updatePhotoMetadata: (photoId: string, data: UpdatePhotoMetadataRequest) =>
    apiClient.put<PhotoResponse>(`/photos/${photoId}/metadata`, data),

  deletePhoto: (photoId: string) =>
    apiClient.delete<void>(`/photos/${photoId}`),

  deletePhotos: (photoIds: string[]) =>
    apiClient.delete<void>('/photos/batch', { data: { photoIds } }),

  deleteAllPhotos: () =>
    apiClient.delete<{ deletedCount: number }>('/photos'),
};
