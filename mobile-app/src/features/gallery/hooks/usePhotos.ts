import { useState, useEffect, useCallback } from 'react';
import { photoApi } from '../../../shared/api/endpoints';
import { Photo } from '../../../shared/types';

export const usePhotos = () => {
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [isFetchingMore, setIsFetchingMore] = useState(false);

  const fetchPhotos = useCallback(async (pageNum: number = 0) => {
    const isInitialLoad = pageNum === 0;

    try {
      if (isInitialLoad) {
        setIsLoading(true);
      } else {
        setIsFetchingMore(true);
      }
      setError(null);

      console.log('Fetching photos, page:', pageNum);
      const response = await photoApi.getPhotos({
        page: pageNum,
        size: 12, // Further reduced to 12 for better stability
        includeDownloadUrl: true
      });

      console.log('Photos response:', response);

      // Backend returns 'items' not 'content'
      const photos = response.data.items || response.data.content || [];
      const hasNext = response.data.hasNext !== undefined ? response.data.hasNext : !response.data.last;

      if (pageNum === 0) {
        setPhotos(photos);
      } else {
        setPhotos(prev => [...prev, ...photos]);
      }

      setHasMore(hasNext);
      setPage(pageNum);
      console.log('Photos loaded:', photos.length, 'Total:', response.data.totalElements);
    } catch (err: any) {
      const errorMsg = err?.response?.data?.message || err?.message || 'Failed to fetch photos';
      setError(errorMsg);
      console.error('Error fetching photos:', err);
      console.error('Error response:', err?.response?.data);
      console.error('Error status:', err?.response?.status);

      // Set empty array on error
      setPhotos([]);
    } finally {
      if (isInitialLoad) {
        setIsLoading(false);
      } else {
        setIsFetchingMore(false);
      }
    }
  }, []);

  const refresh = useCallback(() => {
    fetchPhotos(0);
  }, [fetchPhotos]);

  const loadMore = useCallback(() => {
    if (!isLoading && !isFetchingMore && hasMore) {
      fetchPhotos(page + 1);
    }
  }, [isLoading, isFetchingMore, hasMore, page, fetchPhotos]);

  const deletePhoto = useCallback(async (photoId: string) => {
    try {
      // Optimistic update
      const previousPhotos = [...photos];
      setPhotos(prev => prev.filter(p => p.id !== photoId));

      // Make API call to delete photo
      await photoApi.deletePhoto(photoId);
      console.log('Photo deleted:', photoId);
    } catch (err) {
      console.error('Error deleting photo:', err);
      // Revert on error
      setPhotos(photos);
      throw err;
    }
  }, [photos]);

  const deleteMultiplePhotos = useCallback(async (photoIds: string[]) => {
    try {
      // Optimistic update
      const previousPhotos = [...photos];
      setPhotos(prev => prev.filter(p => !photoIds.includes(p.id)));

      // Make API call to delete photos
      await photoApi.deletePhotos(photoIds);
      console.log('Photos deleted:', photoIds.length);
    } catch (err) {
      console.error('Error deleting photos:', err);
      // Revert on error
      setPhotos(photos);
      throw err;
    }
  }, [photos]);

  useEffect(() => {
    fetchPhotos(0);
  }, [fetchPhotos]);

  return {
    photos,
    isLoading,
    isFetchingMore,
    error,
    hasMore,
    refresh,
    loadMore,
    deletePhoto,
    deleteMultiplePhotos,
  };
};
