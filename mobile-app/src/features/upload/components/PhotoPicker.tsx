import React, { useState } from 'react';
import { View, TouchableOpacity, Text, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import * as ImageManipulator from 'expo-image-manipulator';

interface PhotoPickerProps {
  onPhotosSelected: (photos: Array<{
    uri: string;
    filename: string;
    fileSize: number;
    mimeType: string;
  }>) => void;
}

export const PhotoPicker: React.FC<PhotoPickerProps> = ({ onPhotosSelected }) => {
  const [converting, setConverting] = useState(false);
  const [convertProgress, setConvertProgress] = useState({ current: 0, total: 0 });

  // Detect HEIC/HEIF - expo-image-picker bug returns .jpg extension for HEIC files!
  // We need to check the actual asset type, not just filename
  const isHEICFile = (asset: ImagePicker.ImagePickerAsset): boolean => {
    const filename = asset.fileName || '';
    const lower = filename.toLowerCase();
    
    // Check actual file extension
    const hasHEICExtension = lower.endsWith('.heic') || lower.endsWith('.heif');
    
    // On iOS, if the file came from Photos app and has no extension or .jpg,
    // it's likely HEIC - we should convert ALL photos from library on iOS for safety
    const isFromIOS = asset.uri.includes('ph://');
    
    return hasHEICExtension || isFromIOS;
  };

  // Convert HEIC to JPEG with performance optimizations
  const convertHEICIfNeeded = async (
    asset: ImagePicker.ImagePickerAsset
  ): Promise<{
    uri: string;
    filename: string;
    fileSize: number;
    mimeType: string;
  }> => {
    const filename = asset.fileName || asset.uri.split('/').pop() || 'image.jpg';
    
    // If not HEIC, return as-is with correct mime type
    if (!isHEICFile(asset)) {
      return {
        uri: asset.uri,
        filename,
        fileSize: asset.fileSize || 0,
        mimeType: asset.mimeType || 'image/jpeg',
      };
    }

    try {
      console.log('[PhotoPicker] Converting HEIC file:', filename);
      
      // Convert HEIC to JPEG WITHOUT resizing to avoid corruption
      const result = await ImageManipulator.manipulateAsync(
        asset.uri,
        [], // NO resize - just format conversion
        {
          format: ImageManipulator.SaveFormat.JPEG,
          compress: 1.0, // Maximum quality to avoid any compression artifacts
        }
      );

      console.log('[PhotoPicker] HEIC conversion successful:', {
        original: filename,
        converted: result.uri,
        width: result.width,
        height: result.height,
      });

      return {
        uri: result.uri,
        filename: filename.replace(/\.heic$/i, '.jpg').replace(/\.heif$/i, '.jpg'),
        fileSize: asset.fileSize || 0,
        mimeType: 'image/jpeg',
      };
    } catch (error) {
      console.error('[PhotoPicker] HEIC conversion failed:', error);
      // Fallback: return original if conversion fails (will likely fail to display)
      return {
        uri: asset.uri,
        filename,
        fileSize: asset.fileSize || 0,
        mimeType: 'image/jpeg',
      };
    }
  };

  // Process photos in batches for optimal performance
  const convertPhotosInBatches = async (
    assets: ImagePicker.ImagePickerAsset[]
  ): Promise<Array<{
    uri: string;
    filename: string;
    fileSize: number;
    mimeType: string;
  }>> => {
    const BATCH_SIZE = 4; // Process 4 photos in parallel
    const results: Array<{
      uri: string;
      filename: string;
      fileSize: number;
      mimeType: string;
    }> = [];

    setConverting(true);
    setConvertProgress({ current: 0, total: assets.length });

    try {
      for (let i = 0; i < assets.length; i += BATCH_SIZE) {
        const batch = assets.slice(i, i + BATCH_SIZE);
        const batchResults = await Promise.all(
          batch.map((asset) => convertHEICIfNeeded(asset))
        );
        results.push(...batchResults);
        setConvertProgress({ current: results.length, total: assets.length });
      }
    } catch (error) {
      console.error('Batch conversion error:', error);
      Alert.alert('Conversion Error', 'Some photos could not be converted. Please try again.');
    } finally {
      setConverting(false);
      setConvertProgress({ current: 0, total: 0 });
    }

    return results;
  };

  const requestPermissions = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'Camera roll permission is required');
      return false;
    }
    return true;
  };

  const pickFromLibrary = async () => {
    const hasPermission = await requestPermissions();
    if (!hasPermission) return;

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: true,
      quality: 1,
    });

    if (!result.canceled && result.assets) {
      // Convert HEIC photos to JPEG with optimizations
      const photos = await convertPhotosInBatches(result.assets);
      onPhotosSelected(photos);
    }
  };

  const pickFromCamera = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'Camera permission is required');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ['images'],
      quality: 1,
    });

    if (!result.canceled && result.assets[0]) {
      // Convert HEIC from camera if needed
      const photos = await convertPhotosInBatches(result.assets);
      onPhotosSelected(photos);
    }
  };

  return (
    <View style={styles.container}>
      {converting && (
        <View style={styles.convertingContainer}>
          <ActivityIndicator size="large" color="#3b82f6" />
          <Text style={styles.convertingText}>
            Converting {convertProgress.current} of {convertProgress.total} photos...
          </Text>
          <Text style={styles.convertingSubtext}>
            Optimizing HEIC images for upload
          </Text>
        </View>
      )}
      <TouchableOpacity
        style={[styles.button, converting && styles.buttonDisabled]}
        onPress={pickFromLibrary}
        disabled={converting}
      >
        <Text style={styles.buttonText}>Choose from Library</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={[styles.button, converting && styles.buttonDisabled]}
        onPress={pickFromCamera}
        disabled={converting}
      >
        <Text style={styles.buttonText}>Take Photo</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { gap: 12 },
  button: {
    backgroundColor: '#3b82f6',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonDisabled: {
    backgroundColor: '#94a3b8',
    opacity: 0.6,
  },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  convertingContainer: {
    backgroundColor: '#eff6ff',
    padding: 20,
    borderRadius: 8,
    alignItems: 'center',
    gap: 12,
    borderWidth: 1,
    borderColor: '#bfdbfe',
  },
  convertingText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1e40af',
    marginTop: 8,
  },
  convertingSubtext: {
    fontSize: 14,
    color: '#64748b',
  },
});
