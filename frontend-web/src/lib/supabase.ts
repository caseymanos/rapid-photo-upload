import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://nhadlfbxbivlhtkbolve.supabase.co';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5oYWRsZmJ4Yml2bGh0a2JvbHZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDk0NzMxMzIsImV4cCI6MjA2NTA0OTEzMn0.c3iSIX3NJv3gX8J1J4MNGKgU6ugv6VJE8ckE8mNc_F4';

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true,
  },
});
