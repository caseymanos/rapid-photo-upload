import { supabase } from './supabase';

export interface AuthResponse {
  userId: string;
  email: string;
  token: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export const supabaseAuth = {
  /**
   * Sign up a new user with email and password
   */
  signUp: async (data: RegisterRequest): Promise<AuthResponse> => {
    const { data: authData, error } = await supabase.auth.signUp({
      email: data.email,
      password: data.password,
    });

    if (error) {
      throw new Error(error.message);
    }

    if (!authData.user || !authData.session) {
      throw new Error('Registration failed - no user data returned');
    }

    return {
      userId: authData.user.id,
      email: authData.user.email!,
      token: authData.session.access_token,
    };
  },

  /**
   * Sign in an existing user with email and password
   */
  signIn: async (data: LoginRequest): Promise<AuthResponse> => {
    const { data: authData, error } = await supabase.auth.signInWithPassword({
      email: data.email,
      password: data.password,
    });

    if (error) {
      throw new Error(error.message);
    }

    if (!authData.user || !authData.session) {
      throw new Error('Login failed - no user data returned');
    }

    return {
      userId: authData.user.id,
      email: authData.user.email!,
      token: authData.session.access_token,
    };
  },

  /**
   * Sign out the current user
   */
  signOut: async (): Promise<void> => {
    const { error } = await supabase.auth.signOut();
    if (error) {
      throw new Error(error.message);
    }
  },

  /**
   * Get the current session
   */
  getSession: async () => {
    const { data: { session }, error } = await supabase.auth.getSession();

    if (error) {
      throw new Error(error.message);
    }

    return session;
  },

  /**
   * Get the current user
   */
  getCurrentUser: async () => {
    const { data: { user }, error } = await supabase.auth.getUser();

    if (error) {
      throw new Error(error.message);
    }

    return user;
  },

  /**
   * Listen to auth state changes
   */
  onAuthStateChange: (callback: (event: string, session: any) => void) => {
    return supabase.auth.onAuthStateChange(callback);
  },
};
