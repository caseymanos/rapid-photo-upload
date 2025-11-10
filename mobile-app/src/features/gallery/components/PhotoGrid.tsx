import React, { useState, useMemo, useCallback } from 'react';
import { TouchableOpacity, StyleSheet, Dimensions, View, Text, ActivityIndicator } from 'react-native';
import { Image } from 'expo-image';
import { FlashList, ListRenderItem } from '@shopify/flash-list';
import { Photo } from '../../../shared/types';

interface Props {
  photos: Photo[];
  onPhotoPress: (photo: Photo) => void;
  onEndReached?: () => void;
  onRefresh?: () => void;
  isRefreshing?: boolean;
  isLoadingMore?: boolean;
  selectionMode?: boolean;
  selectedPhotos?: Set<string>;
  onPhotoSelect?: (photoId: string) => void;
}

const { width } = Dimensions.get('window');
const SPACING = 2;
const NUM_COLUMNS = 3;
const ITEM_SIZE = (width - (SPACING * (NUM_COLUMNS + 1))) / NUM_COLUMNS;

const DEV_LOGGING = __DEV__;

const PhotoItemComponent: React.FC<{
  item: Photo;
  onPress: () => void;
  selectionMode?: boolean;
  isSelected?: boolean;
}> = ({ item, onPress, selectionMode, isSelected }) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  // Debug logging
  React.useEffect(() => {
    if (!DEV_LOGGING) return;
    console.log('[PhotoGrid] Photo item:', {
      id: item.id,
      filename: item.originalFilename,
      hasUrl: !!item.downloadUrl,
    });
  }, [item.downloadUrl]);

  return (
    <TouchableOpacity
      style={styles.photoContainer}
      onPress={onPress}
      activeOpacity={0.8}
    >
      {loading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="small" color="#3b82f6" />
        </View>
      )}
      {error ? (
        <View style={styles.errorImageContainer}>
          <Text style={styles.errorImageText}>⚠️</Text>
          <Text style={styles.errorImageSubtext}>Failed</Text>
        </View>
      ) : (
        <Image
          source={{ uri: item.thumbnailUrl || item.thumbnailFallbackUrl || item.downloadUrl }}
          style={[
            styles.photo,
            selectionMode && isSelected && styles.selectedPhoto,
          ]}
          contentFit="cover"
          transition={150}
          cachePolicy="disk"
          recyclingKey={item.id}
          priority="low"
          placeholder={{ blurhash: item.placeholderBase64 || 'L6PZfSjE.AyE_3t7t7R**0o#DgR4' }}
          placeholderContentFit="cover"
          memoryPolicy="discardUnusedMemoryAfterFiveSeconds"
          responsivePolicy="initial"
          onLoadStart={() => {
            if (DEV_LOGGING) {
              console.log('[PhotoGrid] Image load start:', item.originalFilename);
            }
            setLoading(true);
          }}
          onLoad={() => {
            if (DEV_LOGGING) {
              console.log('[PhotoGrid] Image load success:', item.originalFilename);
            }
            setLoading(false);
          }}
          onError={(error) => {
            console.error('[PhotoGrid] Image load error:', {
              filename: item.originalFilename,
              url: item.thumbnailUrl || item.downloadUrl,
              error,
            });
            setLoading(false);
            setError(true);
          }}
        />
      )}
      {selectionMode && (
        <View style={[styles.selectionBadge, isSelected && styles.selectionBadgeActive]}>
          <Text style={styles.selectionText}>{isSelected ? '✓' : ''}</Text>
        </View>
      )}
      {!selectionMode && item.tags && item.tags.length > 0 && (
        <View style={styles.tagBadge}>
          <Text style={styles.tagText}>{item.tags.length}</Text>
        </View>
      )}
    </TouchableOpacity>
  );
};

const PhotoItem = React.memo(PhotoItemComponent);
const EMPTY_SELECTION: ReadonlySet<string> = Object.freeze(new Set<string>());

export const PhotoGrid: React.FC<Props> = ({
  photos = [],
  onPhotoPress,
  onEndReached,
  onRefresh,
  isRefreshing = false,
  isLoadingMore = false,
  selectionMode = false,
  selectedPhotos,
  onPhotoSelect,
}) => {
  const memoizedPhotos = useMemo(() => photos ?? [], [photos]);
  const selectedSet = useMemo(() => selectedPhotos ?? EMPTY_SELECTION, [selectedPhotos]);

  const handlePressFactory = useCallback(
    (item: Photo) => () => {
      if (selectionMode && onPhotoSelect) {
        onPhotoSelect(item.id);
      } else {
        onPhotoPress(item);
      }
    },
    [selectionMode, onPhotoSelect, onPhotoPress]
  );

  const renderItem: ListRenderItem<Photo> = useCallback(
    ({ item }) => (
      <PhotoItem
        item={item}
        onPress={handlePressFactory(item)}
        selectionMode={selectionMode}
        isSelected={selectedSet.has(item.id)}
      />
    ),
    [handlePressFactory, selectionMode, selectedSet]
  );

  const keyExtractor = useCallback((item: Photo) => item.id, []);

  const renderEmpty = useCallback(
    () => (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>No photos yet</Text>
        <Text style={styles.emptySubtext}>Upload some photos to get started</Text>
      </View>
    ),
    []
  );

  const handleEndReached = useCallback(() => {
    if (onEndReached) {
      onEndReached();
    }
  }, [onEndReached]);

  const footerComponent = useMemo(() => {
    if (!isLoadingMore) {
      return null;
    }
    return (
      <View style={styles.footerLoading}>
        <ActivityIndicator size="small" color="#3b82f6" />
      </View>
    );
  }, [isLoadingMore]);

  return (
    <FlashList
      data={memoizedPhotos}
      renderItem={renderItem}
      keyExtractor={keyExtractor}
      numColumns={NUM_COLUMNS}
      contentContainerStyle={styles.container}
      onEndReached={handleEndReached}
      onEndReachedThreshold={0.5}
      onRefresh={onRefresh}
      refreshing={isRefreshing}
      ListEmptyComponent={renderEmpty}
      ListFooterComponent={footerComponent}
      estimatedItemSize={ITEM_SIZE}
      drawDistance={ITEM_SIZE * 6}
      overrideItemLayout={(layout, item) => {
        layout.size = ITEM_SIZE;
      }}
      removeClippedSubviews={true}
    />
  );
};


const styles = StyleSheet.create({
  container: {
    padding: SPACING,
  },
  photoContainer: {
    width: ITEM_SIZE,
    height: ITEM_SIZE,
    margin: SPACING,
    position: 'relative',
  },
  photo: {
    width: '100%',
    height: '100%',
    borderRadius: 4,
  },
  tagBadge: {
    position: 'absolute',
    top: 4,
    right: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  tagText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 80,
  },
  emptyText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#6b7280',
    marginBottom: 8,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#9ca3af',
  },
  loadingContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f3f4f6',
  },
  errorImageContainer: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fef2f2',
    borderRadius: 4,
    borderWidth: 1,
    borderColor: '#fecaca',
  },
  errorImageText: {
    fontSize: 24,
    marginBottom: 4,
  },
  errorImageSubtext: {
    fontSize: 10,
    color: '#ef4444',
    fontWeight: '600',
  },
  selectedPhoto: {
    opacity: 0.6,
    borderWidth: 3,
    borderColor: '#3b82f6',
  },
  selectionBadge: {
    position: 'absolute',
    top: 6,
    right: 6,
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderWidth: 2,
    borderColor: '#d1d5db',
    justifyContent: 'center',
    alignItems: 'center',
  },
  selectionBadgeActive: {
    backgroundColor: '#3b82f6',
    borderColor: '#3b82f6',
  },
  selectionText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  footerLoading: {
    paddingVertical: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
