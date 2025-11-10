import React, { useState } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Dimensions,
  SafeAreaView,
  Alert,
} from 'react-native';
import { Image } from 'expo-image';
import { Photo } from '../../../shared/types';

interface Props {
  photo: Photo | null;
  visible: boolean;
  onClose: () => void;
  onDelete?: (photoId: string) => Promise<void>;
}

const { width, height } = Dimensions.get('window');

export const PhotoModal: React.FC<Props> = ({ photo, visible, onClose, onDelete }) => {
  const [isDeleting, setIsDeleting] = useState(false);

  if (!photo) return null;

  const handleDelete = async () => {
    if (!onDelete) return;

    if (isDeleting) return;

    try {
      setIsDeleting(true);
      await onDelete(photo.id);
      onClose();
    } catch (error) {
      console.error('Error deleting photo:', error);
      Alert.alert('Delete Failed', 'Failed to delete photo. Please try again.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Modal
      visible={visible}
      transparent={true}
      animationType="fade"
      onRequestClose={onClose}
    >
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity onPress={onClose} style={styles.closeButton}>
            <Text style={styles.closeText}>✕</Text>
          </TouchableOpacity>
          {onDelete && (
            <TouchableOpacity
              onPress={handleDelete}
              style={[styles.deleteButton, isDeleting && styles.deletingButton]}
              disabled={isDeleting}
            >
              <Text style={styles.deleteText}>
                {isDeleting ? '...' : '🗑️ Delete'}
              </Text>
            </TouchableOpacity>
          )}
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          <Image
            source={photo.downloadUrl}
            style={styles.image}
            contentFit="contain"
            cachePolicy="memory-disk"
            transition={300}
          />

          <View style={styles.infoContainer}>
            <Text style={styles.filename}>{photo.originalFilename}</Text>

            <View style={styles.metadata}>
              <Text style={styles.metadataLabel}>Size:</Text>
              <Text style={styles.metadataValue}>
                {(photo.fileSizeBytes / 1024 / 1024).toFixed(2)} MB
              </Text>
            </View>

            <View style={styles.metadata}>
              <Text style={styles.metadataLabel}>Type:</Text>
              <Text style={styles.metadataValue}>{photo.mimeType}</Text>
            </View>

            <View style={styles.metadata}>
              <Text style={styles.metadataLabel}>Uploaded:</Text>
              <Text style={styles.metadataValue}>
                {photo.createdAt ? new Date(photo.createdAt).toLocaleString() : 'Unknown'}
              </Text>
            </View>

            {photo.tags && photo.tags.length > 0 && (
              <View style={styles.tagsContainer}>
                <Text style={styles.tagsLabel}>Tags:</Text>
                <View style={styles.tags}>
                  {photo.tags.map((tag, index) => (
                    <View key={index} style={styles.tag}>
                      <Text style={styles.tagText}>{tag}</Text>
                    </View>
                  ))}
                </View>
              </View>
            )}
          </View>
        </ScrollView>
      </SafeAreaView>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.95)',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
  },
  closeButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  closeText: {
    color: '#fff',
    fontSize: 24,
    fontWeight: '300',
  },
  deleteButton: {
    backgroundColor: 'rgba(239, 68, 68, 0.9)',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
  },
  deletingButton: {
    backgroundColor: 'rgba(156, 163, 175, 0.9)',
  },
  deleteText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  content: {
    flexGrow: 1,
  },
  image: {
    width: width,
    height: height * 0.5,
  },
  infoContainer: {
    padding: 16,
  },
  filename: {
    fontSize: 18,
    fontWeight: '600',
    color: '#fff',
    marginBottom: 16,
  },
  metadata: {
    flexDirection: 'row',
    marginBottom: 8,
  },
  metadataLabel: {
    fontSize: 14,
    color: '#9ca3af',
    width: 80,
  },
  metadataValue: {
    fontSize: 14,
    color: '#fff',
    flex: 1,
  },
  tagsContainer: {
    marginTop: 16,
  },
  tagsLabel: {
    fontSize: 14,
    color: '#9ca3af',
    marginBottom: 8,
  },
  tags: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  tag: {
    backgroundColor: 'rgba(59, 130, 246, 0.2)',
    borderWidth: 1,
    borderColor: '#3b82f6',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  tagText: {
    color: '#3b82f6',
    fontSize: 14,
    fontWeight: '500',
  },
});
