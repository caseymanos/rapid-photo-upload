import { create } from 'zustand';
import { authApi } from '../../../shared/api/endpoints';
import { apiClient } from '../../../shared/api/apiClient';
import storage from '../../../shared/utils/storage';
import { LoginRequest, RegisterRequest } from '../../../shared/types';

interface AuthState {
  isAuthenticated: boolean;
  userId: string | null;
  email: string | null;
  isLoading: boolean;
  error: string | null;

  // Actions
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
  checkAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  userId: null,
  email: null,
  isLoading: false,
  error: null,

  login: async (data: LoginRequest) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.login(data);
      const { token, userId, email } = response.data;

      await apiClient.setToken(token);
      await storage.setUserId(userId);
      await storage.setUserEmail(email);

      set({
        isAuthenticated: true,
        userId,
        email,
        isLoading: false,
      });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Login failed';
      set({ error: errorMessage, isLoading: false });
      throw error;
    }
  },

  register: async (data: RegisterRequest) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.register(data);
      const { token, userId, email } = response.data;

      await apiClient.setToken(token);
      await storage.setUserId(userId);
      await storage.setUserEmail(email);

      set({
        isAuthenticated: true,
        userId,
        email,
        isLoading: false,
      });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Registration failed';
      set({ error: errorMessage, isLoading: false });
      throw error;
    }
  },

  logout: async () => {
    await authApi.logout();
    await storage.clearAuth();

    set({
      isAuthenticated: false,
      userId: null,
      email: null,
      error: null,
    });
  },

  clearError: () => set({ error: null }),

  checkAuth: async () => {
    const token = apiClient.getToken();
    const userId = await storage.getUserId();
    const email = await storage.getUserEmail();

    if (token && userId && email) {
      set({ isAuthenticated: true, userId, email });
    } else {
      set({ isAuthenticated: false, userId: null, email: null });
    }
  },
}));
