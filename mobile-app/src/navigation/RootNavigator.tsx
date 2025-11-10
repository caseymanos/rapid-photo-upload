import React from 'react';
import { useAuthStore } from '../features/auth/store/authStore';
import { AuthNavigator } from './AuthNavigator';
import { MainNavigator } from './MainNavigator';

export const RootNavigator: React.FC = () => {
  const { isAuthenticated } = useAuthStore();

  return isAuthenticated ? <MainNavigator /> : <AuthNavigator />;
};
