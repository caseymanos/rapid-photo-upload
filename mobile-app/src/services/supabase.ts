import { createClient } from '@supabase/supabase-js';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants from 'expo-constants';

const supabaseUrl = Constants.expoConfig?.extra?.supabaseUrl || 'https://nhadlfbxbivlhtkbolve.supabase.co';
const supabaseAnonKey = Constants.expoConfig?.extra?.supabaseAnonKey || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5oYWRsZmJ4Yml2bGh0a2JvbHZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDk0NzMxMzIsImV4cCI6MjA2NTA0OTEzMn0.c3iSIX3NJv3gX8J1J4MNGKgU6ugv6VJE8ckE8mNc_F4';

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    storage: AsyncStorage,
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: false,
  },
});
