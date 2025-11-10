import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * AsyncStorage wrapper utility for type-safe storage operations
 */

const STORAGE_KEYS = {
  AUTH_TOKEN: 'auth_token',
  USER_ID: 'user_id',
  USER_EMAIL: 'user_email',
  UPLOAD_QUEUE: 'upload_queue',
} as const;

export const storage = {
  // Auth-related storage
  async setToken(token: string): Promise<void> {
    await AsyncStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
  },

  async getToken(): Promise<string | null> {
    return await AsyncStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
  },

  async setUserId(userId: string): Promise<void> {
    await AsyncStorage.setItem(STORAGE_KEYS.USER_ID, userId);
  },

  async getUserId(): Promise<string | null> {
    return await AsyncStorage.getItem(STORAGE_KEYS.USER_ID);
  },

  async setUserEmail(email: string): Promise<void> {
    await AsyncStorage.setItem(STORAGE_KEYS.USER_EMAIL, email);
  },

  async getUserEmail(): Promise<string | null> {
    return await AsyncStorage.getItem(STORAGE_KEYS.USER_EMAIL);
  },

  async clearAuth(): Promise<void> {
    await AsyncStorage.multiRemove([
      STORAGE_KEYS.AUTH_TOKEN,
      STORAGE_KEYS.USER_ID,
      STORAGE_KEYS.USER_EMAIL,
    ]);
  },

  // Upload queue storage
  async setUploadQueue(queue: any[]): Promise<void> {
    await AsyncStorage.setItem(STORAGE_KEYS.UPLOAD_QUEUE, JSON.stringify(queue));
  },

  async getUploadQueue(): Promise<any[]> {
    const queueJson = await AsyncStorage.getItem(STORAGE_KEYS.UPLOAD_QUEUE);
    return queueJson ? JSON.parse(queueJson) : [];
  },

  async clearUploadQueue(): Promise<void> {
    await AsyncStorage.removeItem(STORAGE_KEYS.UPLOAD_QUEUE);
  },

  // Generic storage methods
  async setItem(key: string, value: string): Promise<void> {
    await AsyncStorage.setItem(key, value);
  },

  async getItem(key: string): Promise<string | null> {
    return await AsyncStorage.getItem(key);
  },

  async removeItem(key: string): Promise<void> {
    await AsyncStorage.removeItem(key);
  },

  async clear(): Promise<void> {
    await AsyncStorage.clear();
  },
};

export default storage;
