import axios, { AxiosInstance, AxiosError } from 'axios';

// Get API base URL from environment variable
// In development: uses Vite's proxy or direct URL
// In production: uses the deployed backend URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

class ApiClient {
  private client: AxiosInstance;
  private token: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 15000, // Reduced from 30s to 15s for faster failures
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add auth token and track request timing
    this.client.interceptors.request.use(
      (config) => {
        if (this.token) {
          config.headers.Authorization = `Bearer ${this.token}`;
        }
        // Add timestamp for performance tracking
        config.headers['X-Request-Start'] = Date.now().toString();
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling and performance logging
    this.client.interceptors.response.use(
      (response) => {
        // Log slow requests for debugging
        const duration = response.config.headers?.['X-Request-Start']
          ? Date.now() - Number(response.config.headers['X-Request-Start'])
          : 0;
        if (duration > 2000) {
          console.warn(`[Performance] Slow API request: ${response.config.url} took ${duration}ms`);
        }
        return response;
      },
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          this.clearToken();
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );

    // Load token from localStorage
    this.loadToken();
  }

  setToken(token: string) {
    this.token = token;
    localStorage.setItem('auth_token', token);
  }

  getToken(): string | null {
    return this.token;
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('auth_token');
  }

  private loadToken() {
    const token = localStorage.getItem('auth_token');
    if (token) {
      this.token = token;
    }
  }

  get<T>(url: string, config?: any) {
    return this.client.get<T>(url, config);
  }

  post<T>(url: string, data?: any, config?: any) {
    return this.client.post<T>(url, data, config);
  }

  put<T>(url: string, data?: any, config?: any) {
    return this.client.put<T>(url, data, config);
  }

  delete<T>(url: string, config?: any) {
    return this.client.delete<T>(url, config);
  }
}

export const apiClient = new ApiClient();
