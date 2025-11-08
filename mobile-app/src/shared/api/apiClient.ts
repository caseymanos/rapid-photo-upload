import axios, { AxiosInstance, AxiosError } from 'axios';
import Constants from 'expo-constants';
import storage from '../utils/storage';

// Get API URL from environment or use default
const API_BASE_URL = Constants.expoConfig?.extra?.apiUrl || 'http://localhost:8080/api/v1';

class ApiClient {
  private client: AxiosInstance;
  private token: string | null = null;
  private onUnauthorized?: () => void;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config) => {
        if (this.token) {
          config.headers.Authorization = `Bearer ${this.token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        if (error.response?.status === 401) {
          await this.clearToken();
          if (this.onUnauthorized) {
            this.onUnauthorized();
          }
        }
        return Promise.reject(error);
      }
    );

    // Load token from AsyncStorage on initialization
    this.loadToken();
  }

  /**
   * Set callback for handling 401 unauthorized responses
   * (e.g., navigate to login screen)
   */
  setUnauthorizedHandler(handler: () => void) {
    this.onUnauthorized = handler;
  }

  async setToken(token: string) {
    this.token = token;
    await storage.setToken(token);
  }

  getToken(): string | null {
    return this.token;
  }

  async clearToken() {
    this.token = null;
    await storage.clearAuth();
  }

  private async loadToken() {
    const token = await storage.getToken();
    if (token) {
      this.token = token;
    }
  }

  /**
   * Ensure token is loaded before making authenticated requests
   * Call this during app initialization
   */
  async initialize() {
    await this.loadToken();
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
