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
} from '../types';

// Auth endpoints
export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>('/auth/register', data),

  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/auth/login', data),

  logout: async () => {
    await apiClient.clearToken();
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
export const photoApi = {
  getPhotos: (page = 0, size = 50) =>
    apiClient.get<PhotoResponse[]>('/photos', {
      params: { page, size },
    }),

  getPhotoById: (photoId: string) =>
    apiClient.get<PhotoResponse>(`/photos/${photoId}`),

  updatePhotoMetadata: (photoId: string, data: UpdatePhotoMetadataRequest) =>
    apiClient.put<PhotoResponse>(`/photos/${photoId}/metadata`, data),
};
