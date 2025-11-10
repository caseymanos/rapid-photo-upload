import { create } from 'zustand';
import { authApi } from '@/shared/api/endpoints';
import { apiClient } from '@/shared/api/apiClient';
import { LoginRequest, RegisterRequest } from '@/shared/types';

interface AuthState {
  isAuthenticated: boolean;
  userId: string | null;
  email: string | null;
  isLoading: boolean;
  error: string | null;

  // Actions
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  clearError: () => void;
  checkAuth: () => void;
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

      apiClient.setToken(token);
      localStorage.setItem('userId', userId);
      localStorage.setItem('email', email);

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

      apiClient.setToken(token);
      localStorage.setItem('userId', userId);
      localStorage.setItem('email', email);

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

  logout: () => {
    authApi.logout();
    localStorage.removeItem('userId');
    localStorage.removeItem('email');

    set({
      isAuthenticated: false,
      userId: null,
      email: null,
      error: null,
    });
  },

  clearError: () => set({ error: null }),

  checkAuth: () => {
    const token = apiClient.getToken();
    const userId = localStorage.getItem('userId');
    const email = localStorage.getItem('email');

    if (token && userId && email) {
      set({ isAuthenticated: true, userId, email });
    }
  },
}));
