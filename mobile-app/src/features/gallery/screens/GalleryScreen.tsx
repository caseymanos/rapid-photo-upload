import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { PhotoGrid } from '../components/PhotoGrid';
import { PhotoModal } from '../components/PhotoModal';
import { usePhotos } from '../hooks/usePhotos';
import { Photo } from '../../../shared/types';
import { LoadingSpinner } from '../../../components/LoadingSpinner';

export const GalleryScreen: React.FC = () => {
  const {
    photos,
    isLoading,
    isFetchingMore,
    error,
    hasMore,
    refresh,
    loadMore,
    deletePhoto,
    deleteMultiplePhotos,
  } = usePhotos();
  const [selectedPhoto, setSelectedPhoto] = useState<Photo | null>(null);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [selectionMode, setSelectionMode] = useState(false);
  const [selectedPhotos, setSelectedPhotos] = useState<Set<string>>(new Set());
  const [isDeleting, setIsDeleting] = useState(false);

  const handlePhotoPress = (photo: Photo) => {
    setSelectedPhoto(photo);
    setIsModalVisible(true);
  };

  const handleCloseModal = () => {
    setIsModalVisible(false);
    setSelectedPhoto(null);
  };

  const toggleSelectionMode = () => {
    setSelectionMode(!selectionMode);
    if (selectionMode) {
      // Exiting selection mode, clear selections
      setSelectedPhotos(new Set());
    }
  };

  const handlePhotoSelect = (photoId: string) => {
    setSelectedPhotos(prev => {
      const newSet = new Set(prev);
      if (newSet.has(photoId)) {
        newSet.delete(photoId);
      } else {
        newSet.add(photoId);
      }
      return newSet;
    });
  };

  const handleSelectAll = () => {
    if (selectedPhotos.size === photos.length) {
      // Deselect all
      setSelectedPhotos(new Set());
    } else {
      // Select all
      setSelectedPhotos(new Set(photos.map(p => p.id)));
    }
  };

  const handleBulkDelete = async () => {
    if (selectedPhotos.size === 0) return;

    try {
      setIsDeleting(true);
      await deleteMultiplePhotos(Array.from(selectedPhotos));
      setSelectedPhotos(new Set());
      setSelectionMode(false);
    } catch (error) {
      console.error('Bulk delete failed:', error);
      Alert.alert('Delete Failed', 'Failed to delete photos. Please try again.');
    } finally {
      setIsDeleting(false);
    }
  };

  if (isLoading && (!photos || photos.length === 0)) {
    return <LoadingSpinner message="Loading photos..." />;
  }

  if (error && (!photos || photos.length === 0)) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.errorContainer}>
          <Text style={styles.errorTitle}>Unable to Load Photos</Text>
          <Text style={styles.errorText}>{error}</Text>
          <TouchableOpacity onPress={refresh} style={styles.retryButton}>
            <Text style={styles.retryText}>Retry</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <View style={styles.headerTop}>
          <View>
            <Text style={styles.title}>Gallery</Text>
            <Text style={styles.count}>
              {selectionMode
                ? `${selectedPhotos.size} selected`
                : `${photos?.length || 0} photos`}
            </Text>
          </View>
          <View style={styles.headerButtons}>
            <TouchableOpacity
              onPress={refresh}
              style={styles.refreshButton}
            >
              <Text style={styles.refreshButtonText}>🔄</Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={toggleSelectionMode}
              style={[styles.selectButton, selectionMode && styles.selectButtonActive]}
            >
              <Text style={[styles.selectButtonText, selectionMode && styles.selectButtonTextActive]}>
                {selectionMode ? 'Cancel' : 'Select'}
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        {selectionMode && (
          <View style={styles.selectionControls}>
            <TouchableOpacity onPress={handleSelectAll} style={styles.controlButton}>
              <Text style={styles.controlButtonText}>
                {selectedPhotos.size === photos.length ? 'Deselect All' : 'Select All'}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={handleBulkDelete}
              disabled={selectedPhotos.size === 0 || isDeleting}
              style={[
                styles.deleteButton,
                (selectedPhotos.size === 0 || isDeleting) && styles.deleteButtonDisabled,
              ]}
            >
              <Text style={styles.deleteButtonText}>
                {isDeleting ? 'Deleting...' : `Delete (${selectedPhotos.size})`}
              </Text>
            </TouchableOpacity>
          </View>
        )}
      </View>

      <PhotoGrid
        photos={photos}
        onPhotoPress={handlePhotoPress}
        onEndReached={loadMore}
        onRefresh={refresh}
        isRefreshing={isLoading && photos.length > 0}
        isLoadingMore={isFetchingMore}
        selectionMode={selectionMode}
        selectedPhotos={selectedPhotos}
        onPhotoSelect={handlePhotoSelect}
      />

      {!selectionMode && (
        <PhotoModal
          photo={selectedPhoto}
          visible={isModalVisible}
          onClose={handleCloseModal}
          onDelete={deletePhoto}
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  header: {
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  headerTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
  },
  count: {
    fontSize: 14,
    color: '#6b7280',
    marginTop: 4,
  },
  headerButtons: {
    flexDirection: 'row',
    gap: 8,
    alignItems: 'center',
  },
  refreshButton: {
    width: 36,
    height: 36,
    borderRadius: 8,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  refreshButtonText: {
    fontSize: 18,
  },
  selectButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#3b82f6',
  },
  selectButtonActive: {
    backgroundColor: '#3b82f6',
  },
  selectButtonText: {
    color: '#3b82f6',
    fontSize: 14,
    fontWeight: '600',
  },
  selectButtonTextActive: {
    color: '#fff',
  },
  selectionControls: {
    flexDirection: 'row',
    marginTop: 12,
    gap: 8,
  },
  controlButton: {
    flex: 1,
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 8,
    backgroundColor: '#f3f4f6',
    alignItems: 'center',
  },
  controlButtonText: {
    color: '#374151',
    fontSize: 14,
    fontWeight: '600',
  },
  deleteButton: {
    flex: 1,
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 8,
    backgroundColor: '#ef4444',
    alignItems: 'center',
  },
  deleteButtonDisabled: {
    backgroundColor: '#d1d5db',
  },
  deleteButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  errorTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 8,
  },
  errorText: {
    fontSize: 14,
    color: '#ef4444',
    textAlign: 'center',
    marginBottom: 24,
  },
  retryButton: {
    backgroundColor: '#3b82f6',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  retryText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
