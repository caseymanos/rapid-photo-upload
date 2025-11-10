import { create } from 'zustand';
import { supabaseAuth } from '../../../services/supabaseAuth';
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
      const { userId, email, token } = await supabaseAuth.signIn(data);

      // Store user data locally
      await storage.setUserId(userId);
      await storage.setUserEmail(email);

      set({
        isAuthenticated: true,
        userId,
        email,
        isLoading: false,
      });
    } catch (error: any) {
      const errorMessage = error.message || 'Login failed';
      set({ error: errorMessage, isLoading: false });
      throw error;
    }
  },

  register: async (data: RegisterRequest) => {
    set({ isLoading: true, error: null });
    try {
      const { userId, email, token } = await supabaseAuth.signUp(data);

      // Store user data locally
      await storage.setUserId(userId);
      await storage.setUserEmail(email);

      set({
        isAuthenticated: true,
        userId,
        email,
        isLoading: false,
      });
    } catch (error: any) {
      const errorMessage = error.message || 'Registration failed';
      set({ error: errorMessage, isLoading: false });
      throw error;
    }
  },

  logout: async () => {
    try {
      await supabaseAuth.signOut();
      await storage.clearAuth();

      set({
        isAuthenticated: false,
        userId: null,
        email: null,
        error: null,
      });
    } catch (error: any) {
      console.error('Logout error:', error);
      // Clear local state even if API call fails
      await storage.clearAuth();
      set({
        isAuthenticated: false,
        userId: null,
        email: null,
        error: null,
      });
    }
  },

  clearError: () => set({ error: null }),

  checkAuth: async () => {
    try {
      const session = await supabaseAuth.getSession();

      if (session?.user) {
        const userId = session.user.id;
        const email = session.user.email!;

        // Update local storage
        await storage.setUserId(userId);
        await storage.setUserEmail(email);

        set({ isAuthenticated: true, userId, email });
      } else {
        set({ isAuthenticated: false, userId: null, email: null });
      }
    } catch (error) {
      console.error('Check auth error:', error);
      set({ isAuthenticated: false, userId: null, email: null });
    }
  },
}));
