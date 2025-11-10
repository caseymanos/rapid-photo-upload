import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { UploadScreen } from '../features/upload/screens/UploadScreen';
import { GalleryScreen } from '../features/gallery/screens/GalleryScreen';
import { Text } from 'react-native';

export type MainTabParamList = {
  Upload: undefined;
  Gallery: undefined;
};

const Tab = createBottomTabNavigator<MainTabParamList>();

export const MainNavigator: React.FC = () => {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#3b82f6',
        tabBarInactiveTintColor: '#6b7280',
      }}
    >
      <Tab.Screen
        name="Upload"
        component={UploadScreen}
        options={{
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 24 }}>📤</Text>,
        }}
      />
      <Tab.Screen
        name="Gallery"
        component={GalleryScreen}
        options={{
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 24 }}>🖼️</Text>,
        }}
      />
    </Tab.Navigator>
  );
};
