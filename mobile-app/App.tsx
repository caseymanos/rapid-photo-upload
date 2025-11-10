import React, { useEffect, useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { apiClient } from './src/shared/api/apiClient';
import { useAuthStore } from './src/features/auth/store/authStore';
import { LoadingSpinner } from './src/components/LoadingSpinner';
import { RootNavigator } from './src/navigation/RootNavigator';

export default function App() {
  const [isReady, setIsReady] = useState(false);
  const { checkAuth } = useAuthStore();

  useEffect(() => {
    async function prepare() {
      try {
        // Initialize API client
        await apiClient.initialize();

        // Check if user is already authenticated
        await checkAuth();
      } catch (error) {
        console.error('Error during app initialization:', error);
      } finally {
        setIsReady(true);
      }
    }

    prepare();
  }, []);

  if (!isReady) {
    return <LoadingSpinner message="Loading..." />;
  }

  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <RootNavigator />
        <StatusBar style="auto" />
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
